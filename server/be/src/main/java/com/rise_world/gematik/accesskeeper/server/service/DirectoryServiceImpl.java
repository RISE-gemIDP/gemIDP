/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.server.dto.RemoteIdpDTO;
import com.rise_world.gematik.accesskeeper.server.model.SektorApp;
import com.rise_world.gematik.accesskeeper.common.token.creation.TokenCreationStrategy;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DirectoryServiceImpl implements DirectoryService {

    private static final String IDP_LIST = "kk_app_list";
    private static final String IDP_ID = "kk_app_id";
    private static final String IDP_NAME = "kk_app_name";

    private static final Logger LOG = LoggerFactory.getLogger(DirectoryServiceImpl.class);

    private ConfigService configuration;
    private RemoteDiscoveryDocumentClient ddClient;
    private TokenCreationStrategy discStrategy;
    private Duration reloadInterval;
    private Duration expirationInterval;

    @SuppressWarnings("java:S3749") // map is synchronized to handle concurrent access
    private Map<String, RemoteIdpDTO> directoryCache = new ConcurrentHashMap<>();

    @Autowired
    public DirectoryServiceImpl(ConfigService configuration,
                                RemoteDiscoveryDocumentClient ddClient,
                                @Qualifier("discStrategy") TokenCreationStrategy discStrategy,
                                @Value("${directoryService.reload.interval}") String reloadInterval,
                                @Value("${directoryService.expiration.interval}") String expirationInterval) {
        this.configuration = configuration;
        this.ddClient = ddClient;
        this.discStrategy = discStrategy;

        this.reloadInterval = Duration.parse(reloadInterval);
        this.expirationInterval = Duration.parse(expirationInterval);
    }

    @Override
    public String getAppDirectory() {
        List<Map<String, Object>> idpList = new ArrayList<>();
        for (SektorApp sektorIdp : this.configuration.getSektorApps()) {
            Map<String, Object> idp = new HashMap<>();
            idp.put(IDP_NAME, sektorIdp.getName());
            idp.put(IDP_ID, sektorIdp.getId());
            idpList.add(idp);
        }

        // A_22284: analog zu A_20591-01 wird die 'discStrategy' verwendet um den Token mit PrK_DISC_SIG zu signieren.
        return discStrategy.toToken(new JwtClaims(Collections.singletonMap(IDP_LIST, idpList)));
    }

    @Override
    public RemoteIdpDTO getRemoteIdpConfig(String kkAppId) {
        if (StringUtils.isEmpty(kkAppId)) {
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_MISSING_KK_APP);
        }

        RemoteIdpDTO remoteIdpDTO = directoryCache.get(kkAppId);
        if (remoteIdpDTO == null) {
            LOG.warn("Unknown/unavailable sektor app idp '{}' was requested", kkAppId);
            if (configuration.getSektorAppById(kkAppId) == null) {
                throw new AccessKeeperException(ErrorCodes.EXTAUTH_UNKNOWN_KK_APP);
            }
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_IDP_NOT_AVAILABLE);
        }

        return remoteIdpDTO;
    }

    @Override
    public void rebuildDirectoryCache() {
        directoryCache.clear();
        updateDirectoryCache();
    }

    /**
     * Timer method for updating the directory cache.
     * <p>
     * The initial delay is 2 minutes
     */
    @PostConstruct
    @Scheduled(fixedDelayString = "${directoryService.timer.delay}", initialDelayString = "${directoryService.timer.initialdelay}")
    public void updateDirectoryCache() {
        LOG.info("Updating directory cache");

        for (SektorApp sektorIdp : configuration.getSektorApps()) {
            RemoteIdpDTO remoteIdpDTO = directoryCache.get(sektorIdp.getId());
            Instant now = Clock.systemUTC().instant();

            // initial fetch
            if (remoteIdpDTO == null) {
                loadDiscoveryDocument(sektorIdp);
            }
            // update
            else if (now.isAfter(remoteIdpDTO.getCreatedAt().plus(reloadInterval))) {
                LOG.info("DD entry for '{}' needs to be reloaded", sektorIdp.getId());
                final boolean success = loadDiscoveryDocument(sektorIdp);

                if (!success && now.isAfter(remoteIdpDTO.getCreatedAt().plus(expirationInterval))) {
                    directoryCache.remove(sektorIdp.getId());
                    LOG.info("DD entry for '{}' was expired after {}", sektorIdp.getId(), expirationInterval);
                }
            }
        }
    }

    private boolean loadDiscoveryDocument(SektorApp sektorIdp) {
        Optional<RemoteIdpDTO> remoteIdpDTO = ddClient.loadDiscoveryDocument(sektorIdp);
        remoteIdpDTO.ifPresent(idpDTO -> directoryCache.put(sektorIdp.getId(), idpDTO));
        return remoteIdpDTO.isPresent();
    }
}

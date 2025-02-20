/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import com.rise_world.gematik.accesskeeper.server.dto.EntityStatementDTO;
import com.rise_world.gematik.accesskeeper.server.dto.OpenidProviderDTO;
import com.rise_world.gematik.accesskeeper.server.dto.ReloadEntityStatementEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
public class EntityStatementSynchronizationServiceImpl implements EntityStatementSynchronizationService {

    public static final String OIDC_PROVIDER_ES_INVALID_SIGNATURE = "Signatur des Entity Statements des sektoralen Identity Provider ist ung\u00fcltig";
    public static final String OIDC_PROVIDER_NOT_AVAILABLE = "Sektoraler Identity Provider ist nicht erreichbar";
    public static final String OIDC_PROVIDER_TOKEN_INVALID = "Antwort des sektoralen Identity Providers ist ung\u00fcltig";
    public static final String FED_MASTER_NOT_AVAILABLE = "Federation Master ist nicht erreichbar";
    public static final String FED_MASTER_INVALID_SIGNATURE = "Signatur des Federation Master Tokens ist ung\u00fcltig";
    public static final String FED_MASTER_INVALID_TOKEN = "Antwort des Federation Masters ist ung\u00fcltig";


    private static final Logger LOG = LoggerFactory.getLogger(EntityStatementSynchronizationServiceImpl.class);

    private final IdentityFederationDirectoryService directoryService;
    private final OpenIdProviderFetcher openIdProviderFetcher;
    private final EntityStatementCache cache;

    public EntityStatementSynchronizationServiceImpl(IdentityFederationDirectoryService directoryService,
                                              OpenIdProviderFetcher openIdProviderFetcher,
                                              EntityStatementCache cache) {
        this.directoryService = directoryService;
        this.openIdProviderFetcher = openIdProviderFetcher;
        this.cache = cache;
    }

    @Override
    @Scheduled(fixedDelayString = "${federation.synchronization.interval}")
    public void updateEntityStatementCache() {
        LOG.info("Updating entity statement cache");

        directoryService.getOpenIdProviders()
            .stream()
            .map(this::synchronizeOpenidProvider)
            .flatMap(Optional::stream)
            .forEach(cache::store);
    }

    private Optional<EntityStatementDTO> synchronizeOpenidProvider(OpenidProviderDTO openidProvider) {

        LogTool.setIdpIss(openidProvider.getIssuer());
        try {
            return Optional.of(openIdProviderFetcher.fetch(openidProvider));
        }
        catch (Exception e) {
            LOG.error("Failed to synchronize openid provider {}", openidProvider.getIssuer(), e);
            return Optional.empty();
        }
        finally {
            LogTool.clearIdpIss();
        }
    }


    @Override
    public Collection<EntityStatementDTO> getEntityStatementCache() {
        return cache.get();
    }

    @Override
    public EntityStatementDTO getEntityStatementCache(String idpIss) {
        return cache.findFirstIssuer(idpIss)
            .orElseThrow(() -> new AccessKeeperException(ErrorCodes.FEDAUTH_MISSING_IDP_ES));
    }

    @EventListener
    @Async
    @Override
    public void reloadEntityStatement(ReloadEntityStatementEvent event) {
        cache.attemptUpdate(event.issuer(), this::synchronizeOpenidProvider);
    }

}

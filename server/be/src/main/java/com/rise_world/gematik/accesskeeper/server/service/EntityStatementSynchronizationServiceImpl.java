/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.common.crypt.KeyProvider;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.service.FederationEndpointProvider;
import com.rise_world.gematik.accesskeeper.common.service.SignedJwksEndpoint;
import com.rise_world.gematik.accesskeeper.common.service.SynchronizationConfiguration;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import com.rise_world.gematik.accesskeeper.server.configuration.FederationMasterConfiguration;
import com.rise_world.gematik.accesskeeper.server.configuration.IdpConstants;
import com.rise_world.gematik.accesskeeper.server.dto.EntityStatementDTO;
import com.rise_world.gematik.accesskeeper.server.dto.OpenidProviderDTO;
import com.rise_world.gematik.accesskeeper.server.exception.EntityStatementSyncException;
import com.rise_world.gematik.idp.server.api.federation.FederationEndpoint;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jwk.PublicKeyUse;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.rise_world.gematik.accesskeeper.common.crypt.KeyConstants.PUK_FEDMASTER_SIG;
import static net.logstash.logback.marker.Markers.append;

@Service
public class EntityStatementSynchronizationServiceImpl implements EntityStatementSynchronizationService {

    public static final String OIDC_PROVIDER_ES_INVALID_SIGNATURE = "Signatur des Entity Statements des sektoralen Identity Provider ist ung\u00fcltig";
    public static final String OIDC_PROVIDER_NOT_AVAILABLE = "Sektoraler Identity Provider ist nicht erreichbar";
    public static final String OIDC_PROVIDER_TOKEN_INVALID = "Antwort des sektoralen Identity Providers ist ung\u00fcltig";
    public static final String FED_MASTER_NOT_AVAILABLE = "Federation Master ist nicht erreichbar";
    public static final String FED_MASTER_INVALID_SIGNATURE = "Signatur des Federation Master Tokens ist ung\u00fcltig";
    public static final String FED_MASTER_INVALID_TOKEN = "Antwort des Federation Masters ist ung\u00fcltig";


    private static final Logger LOG = LoggerFactory.getLogger(EntityStatementSynchronizationServiceImpl.class);
    private static final JsonMapObjectReaderWriter JWKS_TO_JSON_WRITER = new JsonMapObjectReaderWriter();

    private final Clock clock;
    private final IdentityFederationDirectoryService directoryService;
    private final FederationEndpointProvider endpointProvider;
    private final FederationEndpoint fedmasterEndpoint;
    private final KeyProvider keyProvider;
    private final SynchronizationConfiguration syncConfig;

    private final String fedmasterIssuer;

    @SuppressWarnings("java:S3749") // map is synchronized to handle concurrent access
    private final Map<String, EntityStatementDTO> entityStatementCache = new ConcurrentHashMap<>();

    @Autowired
    public EntityStatementSynchronizationServiceImpl(Clock clock,
                                                     IdentityFederationDirectoryService directoryService,
                                                     FederationEndpointProvider endpointProvider,
                                                     FederationEndpoint fedmasterEndpoint,
                                                     KeyProvider keyProvider,
                                                     SynchronizationConfiguration syncConfig,
                                                     FederationMasterConfiguration fedMasterConfig) {
        this.clock = clock;
        this.directoryService = directoryService;
        this.endpointProvider = endpointProvider;
        this.fedmasterEndpoint = fedmasterEndpoint;
        this.keyProvider = keyProvider;
        this.syncConfig = syncConfig;
        this.fedmasterIssuer = fedMasterConfig.getIssuer();
    }

    @Override
    @Scheduled(fixedDelayString = "${federation.synchronization.interval}")
    public void updateEntityStatementCache() {
        LOG.info("Updating entity statement cache");

        for (OpenidProviderDTO openidProvider : directoryService.getOpenIdProviders()) {
            try {
                synchronizeOpenidProvider(openidProvider);
            }
            catch (Exception e) {
                LOG.error(append("idp_iss", openidProvider.getIssuer()),
                        "Failed to synchronize openid provider {}", openidProvider.getIssuer(), e);
            }
        }
    }

    /**
     * Synchronize the entity statement for the given openid provider.
     *
     * @param openidProvider not null
     */
    protected void synchronizeOpenidProvider(OpenidProviderDTO openidProvider) {
        String issuer = openidProvider.getIssuer();
        LOG.info("Synchronize openid provider {}", issuer);

        String masterES = makeRemoteCall(() -> fedmasterEndpoint.fetchEndpoint(fedmasterIssuer, issuer, null), ErrorCodes.FED_MASTER_NOT_AVAILABLE.getText());
        JsonWebKeys masterJsonWebKeys = validateMasterEntityStatement(masterES);

        String providerES = makeRemoteCall(() -> endpointProvider.create(issuer, IdpConstants.USER_AGENT).getFederationEntity(), OIDC_PROVIDER_NOT_AVAILABLE);
        JwtClaims openidProviderEs = validateProviderEntityStatement(masterJsonWebKeys, providerES);
        EntityStatementDTO entityStatementDTO = toEntityStatementDTO(masterJsonWebKeys, openidProvider, openidProviderEs);

        LOG.info("Add openid provider {} with expiry {} to entity statement cache", issuer, entityStatementDTO.getExp());
        entityStatementCache.put(issuer, entityStatementDTO);
    }

    private JsonWebKeys validateMasterEntityStatement(String masterEntityStatement) {
        JwsJwtCompactConsumer consumer;
        JwtClaims jwtClaims;
        try {
            consumer = new IdpJwsJwtCompactConsumer(masterEntityStatement.trim());
            jwtClaims = consumer.getJwtClaims();
        }
        catch (Exception e) {
            throw new EntityStatementSyncException(ErrorCodes.FED_TOKEN_INVALID.getText(), e);
        }
        if (!consumer.verifySignatureWith(keyProvider.getKey(PUK_FEDMASTER_SIG), SignatureAlgorithm.ES256)) {
            throw new EntityStatementSyncException(ErrorCodes.FED_INVALID_MASTER_SIGNATURE.getText());
        }
        JsonMapObject jwksClaims = jwtClaims.getJsonMapProperty("jwks");

        if (jwksClaims == null) {
            LOG.error("Missing public key in entity statement from federation master");
            throw new EntityStatementSyncException(ErrorCodes.FED_TOKEN_INVALID.getText());
        }
        String jsonJwks = JWKS_TO_JSON_WRITER.toJson(jwksClaims);
        return JwkUtils.readJwkSet(jsonJwks);
    }

    private JwtClaims validateProviderEntityStatement(JsonWebKeys masterJsonWebKeys, String openidProviderES) {
        JwsJwtCompactConsumer consumer;
        try {
            consumer = new IdpJwsJwtCompactConsumer(openidProviderES.trim());
            consumer.getJwtClaims();  // trigger token parsing
        }
        catch (Exception e) {
            throw new EntityStatementSyncException(OIDC_PROVIDER_TOKEN_INVALID, e);
        }
        JsonWebKey sekIdpPukFromFedMaster = masterJsonWebKeys.getKeys().stream()
            .filter(key -> key.getKeyId().equals(consumer.getJwsHeaders().getKeyId()))
            .findFirst()
            .orElseThrow(() -> {
                LOG.error("Missing jwk with kid {} in openid provider ES from federation master", consumer.getJwsHeaders().getKeyId());
                return new EntityStatementSyncException(ErrorCodes.FED_TOKEN_INVALID.getText());
            });

        // @AFO: A_23040 pruefen der signatur
        if (!consumer.verifySignatureWith(sekIdpPukFromFedMaster)) {
            throw new EntityStatementSyncException(OIDC_PROVIDER_ES_INVALID_SIGNATURE);
        }
        return consumer.getJwtClaims();
    }

    private EntityStatementDTO toEntityStatementDTO(JsonWebKeys masterJsonWebKeys, OpenidProviderDTO parent, JwtClaims providerEs) {
        EntityStatementDTO dto = new EntityStatementDTO(parent.getIssuer(), parent.getOrganizationName(), parent.getLogoUri(), parent.isPkv());
        dto.setCreatedAt(clock.instant());

        try {
            assertEntries(providerEs.asMap(), "exp", "metadata", "jwks");

            long effectiveExp = Math.min(dto.getCreatedAt().plus(syncConfig.getExpiration()).getEpochSecond(), providerEs.getExpiryTime());
            dto.setExp(effectiveExp);

            JsonMapObject metadata = providerEs.getJsonMapProperty("metadata");
            assertEntries(metadata.asMap(), "openid_provider");

            JsonMapObject openidProvider = metadata.getJsonMapProperty("openid_provider");
            assertEndpointUrls(openidProvider.asMap(), "token_endpoint", "pushed_authorization_request_endpoint", "authorization_endpoint");

            String tokenEndpoint = openidProvider.getStringProperty("token_endpoint");
            dto.setTokenEndpoint(tokenEndpoint);

            String parEndpoint = openidProvider.getStringProperty("pushed_authorization_request_endpoint");
            dto.setPushedAuthorizationRequestEndpoint(parEndpoint);

            String authEndpoint = openidProvider.getStringProperty("authorization_endpoint");
            dto.setAuthorizationEndpoint(authEndpoint);

            // load ID_TOKEN signature keys from jwks claims
            JsonMapObject jwksClaims = providerEs.getJsonMapProperty("jwks");
            String jsonJwks = JWKS_TO_JSON_WRITER.toJson(jwksClaims);
            JsonWebKeys providerJsonWebKeys = JwkUtils.readJwkSet(jsonJwks);

            List<JsonWebKey> keys = providerJsonWebKeys.getKeys(); // returns null in case of an empty list...
            if (keys != null) {
                keys.forEach(key -> {
                    if (key.getPublicKeyUse() == PublicKeyUse.SIGN) {
                        dto.getKeys().put(key.getKeyId(), key);
                    }
                });
            }

            // load ID_TOKEN signature keys from signed_jwks_uri
            if (openidProvider.getProperty("signed_jwks_uri") instanceof String jwksUri) {
                SignedJwksEndpoint webClient = endpointProvider.createJwksEndpoint(jwksUri, IdpConstants.USER_AGENT);
                String token = makeRemoteCall(webClient::getSignedJwks, OIDC_PROVIDER_NOT_AVAILABLE);

                List<JsonWebKey> jsonWebKeys = validateSignedJwksToken(masterJsonWebKeys, providerJsonWebKeys, token);
                jsonWebKeys.forEach(key -> dto.getKeys().put(key.getKeyId(), key));
            }
            Validate.isTrue(dto.getKeys().size() > 0, "missing jwks with use=sign");
        }
        catch (IllegalArgumentException | MalformedURLException e) {
            throw new EntityStatementSyncException(OIDC_PROVIDER_TOKEN_INVALID, e);
        }
        return dto;
    }

    private static List<JsonWebKey> validateSignedJwksToken(JsonWebKeys masterJsonWebKeys, JsonWebKeys providerJsonWebKeys, String token) {
        JwsJwtCompactConsumer consumer;
        try {
            consumer = new IdpJwsJwtCompactConsumer(token.trim());
            consumer.getJwtClaims();  // trigger token parsing
        }
        catch (Exception e) {
            throw new EntityStatementSyncException(OIDC_PROVIDER_TOKEN_INVALID, e);
        }
        JsonWebKey signatureKey = masterJsonWebKeys.getKeys().stream()
            .filter(key -> key.getKeyId().equals(consumer.getJwsHeaders().getKeyId()))
            .findFirst()
            .or(() -> providerJsonWebKeys.getKeys().stream()
                .filter(key -> key.getKeyId().equals(consumer.getJwsHeaders().getKeyId()))
                .findFirst())
            .orElseThrow(() -> {
                LOG.error("Missing jwk with kid {} in openid provider ES", consumer.getJwsHeaders().getKeyId());
                return new EntityStatementSyncException(OIDC_PROVIDER_TOKEN_INVALID);
            });
        if (!consumer.verifySignatureWith(signatureKey)) {
            throw new EntityStatementSyncException(OIDC_PROVIDER_TOKEN_INVALID);
        }
        List<Map<String, Object>> keys = consumer.getJwtClaims().getListMapProperty("keys");
        if (keys != null) {
            return keys.stream()
                .map(JsonWebKey::new)
                .filter(key -> key.getPublicKeyUse() == PublicKeyUse.SIGN)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private String makeRemoteCall(Supplier<String> supplier, String errorMessage) {
        try {
            return supplier.get();
        }
        catch (WebApplicationException e) {
            throw new EntityStatementSyncException(errorMessage, e);
        }
        catch (ProcessingException e) {
            // handle timeout when remote service not available
            if ((e.getCause() instanceof SocketTimeoutException) || (e.getCause() instanceof ConnectException) ||
                (e.getCause() instanceof NoRouteToHostException) || (e.getCause() instanceof UnknownHostException)) {
                throw new EntityStatementSyncException(errorMessage, e);
            }
            else {
                throw e;
            }
        }
    }

    private void assertEntries(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Validate.isTrue(map.get(key) != null, String.format("ES does not contain entry %s", key));
        }
    }

    private void assertEndpointUrls(Map<?, ?> map, String... keys) throws MalformedURLException {
        for (String key : keys) {
            Validate.isTrue(map.containsKey(key), String.format("ES does not contain entry %s", key));
            Validate.isInstanceOf(String.class, map.get(key));
            URL endpointUrl = new URL((String) map.get(key));
            Validate.isTrue("https".equals(endpointUrl.getProtocol()));
        }
    }

    @Override
    public Collection<EntityStatementDTO> getEntityStatementCache() {
        long now = clock.instant().getEpochSecond();
        List<EntityStatementDTO> result = new ArrayList<>();
        Iterator<EntityStatementDTO> iterator = entityStatementCache.values().iterator();

        while (iterator.hasNext()) {
            EntityStatementDTO next = iterator.next();

            if (next.getExp() > now) {
                result.add(next);
            }
            else {
                LOG.info("Cache entry for openid provider {} expired, remove from entity statement cache", next.getIssuer());
                iterator.remove();
            }
        }
        return result;
    }

    @Override
    public EntityStatementDTO getEntityStatementCache(String idpIss) {
        return getEntityStatementCache().stream()
            .filter(es -> es.getIssuer().equals(idpIss))
            .findFirst()
            .orElseThrow(() -> {
                LOG.error("Issuer {} not present in entity statement cache", idpIss);
                return new AccessKeeperException(ErrorCodes.FEDAUTH_MISSING_IDP_ES);
            });
    }

}

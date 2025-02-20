/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.common.crypt.KeyProvider;
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
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.stream.Streams;
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
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.rise_world.gematik.accesskeeper.common.crypt.KeyConstants.PUK_FEDMASTER_SIG;
import static com.rise_world.gematik.accesskeeper.server.service.EntityStatementSynchronizationServiceImpl.OIDC_PROVIDER_ES_INVALID_SIGNATURE;
import static com.rise_world.gematik.accesskeeper.server.service.EntityStatementSynchronizationServiceImpl.OIDC_PROVIDER_NOT_AVAILABLE;
import static com.rise_world.gematik.accesskeeper.server.service.EntityStatementSynchronizationServiceImpl.OIDC_PROVIDER_TOKEN_INVALID;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Service
public class OpenIdProviderFetcher {

    private static final Logger LOG = LoggerFactory.getLogger(OpenIdProviderFetcher.class);

    private static final JsonMapObjectReaderWriter JWKS_TO_JSON_WRITER = new JsonMapObjectReaderWriter();

    private final Clock clock;
    private final FederationEndpointProvider endpointProvider;
    private final FederationEndpoint fedmasterEndpoint;
    private final KeyProvider keyProvider;
    private final SynchronizationConfiguration syncConfig;

    private final String fedmasterIssuer;

    public OpenIdProviderFetcher(Clock clock,
                                 FederationEndpointProvider endpointProvider,
                                 FederationEndpoint fedmasterEndpoint,
                                 KeyProvider keyProvider,
                                 SynchronizationConfiguration syncConfig,
                                 FederationMasterConfiguration fedmasterConfig) {
        this.clock = clock;
        this.endpointProvider = endpointProvider;
        this.fedmasterEndpoint = fedmasterEndpoint;
        this.keyProvider = keyProvider;
        this.syncConfig = syncConfig;
        this.fedmasterIssuer = fedmasterConfig.getIssuer();
    }

    /**
     * {@code fetch} loads and validates the {@link EntityStatementDTO entity statement} for the given {@link OpenidProviderDTO openID provider}.
     * if validation fails an {@link EntityStatementSyncException} will be thrown.
     *
     * @param openidProvider {@link OpenidProviderDTO openID provider} to load
     * @return {@link EntityStatementDTO entity statement} for the given {@link OpenidProviderDTO openID provider}
     */
    public EntityStatementDTO fetch(OpenidProviderDTO openidProvider) {
        String issuer = openidProvider.getIssuer();
        LOG.info("Synchronize openid provider {}", issuer);

        String masterES = makeRemoteCall(() -> fedmasterEndpoint.fetchEndpoint(fedmasterIssuer, issuer, null), ErrorCodes.FED_MASTER_NOT_AVAILABLE.getText());
        JsonWebKeys masterJsonWebKeys = validateMasterEntityStatement(masterES);

        String providerES = makeRemoteCall(() -> endpointProvider.create(issuer, IdpConstants.USER_AGENT).getFederationEntity(), OIDC_PROVIDER_NOT_AVAILABLE);
        JwtClaims openidProviderEs = validateProviderEntityStatement(masterJsonWebKeys, providerES);
        EntityStatementDTO entityStatementDTO = toEntityStatementDTO(masterJsonWebKeys, openidProvider, openidProviderEs);

        LOG.info("successfully fetched entity statement of openid provider {}", issuer);
        return entityStatementDTO;
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
        JsonWebKeys jwtSet = JwkUtils.readJwkSet(jsonJwks);
        if (isNull(jwtSet.getKeys())) {
            LOG.error("missing keys in masterJsonWebKeys");
            throw new EntityStatementSyncException(ErrorCodes.FED_TOKEN_INVALID.getText());
        }

        return jwtSet;
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
            assertEntries(providerEs, "exp", "metadata", "jwks");

            long effectiveExp = Math.min(dto.getCreatedAt().plus(syncConfig.getExpiration()).getEpochSecond(), providerEs.getExpiryTime());
            dto.setExp(effectiveExp);

            JsonMapObject metadata = providerEs.getJsonMapProperty("metadata");
            assertEntries(metadata, "openid_provider");

            JsonMapObject openidProvider = metadata.getJsonMapProperty("openid_provider");
            assertEndpointUrls(openidProvider, "token_endpoint", "pushed_authorization_request_endpoint", "authorization_endpoint");

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

            dto.getKeys().putAll(Streams.of(providerJsonWebKeys.getKeys())
                .filter(OpenIdProviderFetcher::isSigningKey)
                .collect(toMap(JsonWebKey::getKeyId, identity(), (key1, key2) -> key1)));

            // load ID_TOKEN signature keys from signed_jwks_uri
            if (openidProvider.getProperty("signed_jwks_uri") instanceof String jwksUri) {
                SignedJwksEndpoint webClient = endpointProvider.createJwksEndpoint(jwksUri, IdpConstants.USER_AGENT);
                String token = makeRemoteCall(webClient::getSignedJwks, OIDC_PROVIDER_NOT_AVAILABLE);

                dto.getKeys().putAll(validateSignedJwksToken(masterJsonWebKeys, providerJsonWebKeys, token));
            }
            Validate.notEmpty(dto.getKeys(), "missing jwks with use=sign");
        }
        catch (IllegalArgumentException | URISyntaxException e) {
            throw new EntityStatementSyncException(OIDC_PROVIDER_TOKEN_INVALID, e);
        }
        return dto;
    }

    private static boolean isSigningKey(JsonWebKey key) {
        return nonNull(key) && key.getPublicKeyUse() == PublicKeyUse.SIGN;
    }

    private static Map<String, JsonWebKey> validateSignedJwksToken(JsonWebKeys masterJsonWebKeys, JsonWebKeys providerJsonWebKeys, String token) {
        JwsJwtCompactConsumer consumer;
        try {
            consumer = new IdpJwsJwtCompactConsumer(token.trim());
            consumer.getJwtClaims();  // trigger token parsing
        }
        catch (Exception e) {
            throw new EntityStatementSyncException(OIDC_PROVIDER_TOKEN_INVALID, e);
        }

        JsonWebKey signatureKey = Stream.concat(
                masterJsonWebKeys.getKeys().stream(),
                providerJsonWebKeys.getKeys().stream())
            .filter(key -> key.getKeyId().equals(consumer.getJwsHeaders().getKeyId()))
            .findFirst()
            .orElseThrow(() -> {
                LOG.error("Missing jwk with kid {} in openid provider ES", consumer.getJwsHeaders().getKeyId());
                return new EntityStatementSyncException(OIDC_PROVIDER_TOKEN_INVALID);
            });

        if (!consumer.verifySignatureWith(signatureKey)) {
            throw new EntityStatementSyncException(OIDC_PROVIDER_TOKEN_INVALID);
        }

        return Streams.of(consumer.getJwtClaims().getListMapProperty("keys"))
            .map(JsonWebKey::new)
            .filter(OpenIdProviderFetcher::isSigningKey)
            .collect(toMap(JsonWebKey::getKeyId, identity(), (key1, key2) -> key1));
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

    private void assertEntries(JsonMapObject claims, String... keys) {
        Stream.of(keys)
            .forEach(key -> Validate.isTrue(nonNull(claims.getProperty(key)), "ES does not contain entry %s", key));
    }

    private void assertEndpointUrls(JsonMapObject claims, String... keys) throws URISyntaxException {
        for (String key : keys) {

            var value = claims.getProperty(key);
            Validate.isTrue(nonNull(value), "ES does not contain entry %s", key);
            Validate.isInstanceOf(String.class, value);

            var endpointUrl = new URI((String) value);
            Validate.isTrue("https".equals(endpointUrl.getScheme()));
        }
    }
}

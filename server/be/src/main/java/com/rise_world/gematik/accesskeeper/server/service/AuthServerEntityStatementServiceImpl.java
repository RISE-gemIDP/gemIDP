/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.common.OAuth2Constants;
import com.rise_world.gematik.accesskeeper.common.crypt.KeyProvider;
import com.rise_world.gematik.accesskeeper.server.configuration.FederationMasterConfiguration;
import com.rise_world.gematik.accesskeeper.server.dto.RequestSource;
import com.rise_world.gematik.accesskeeper.server.token.creation.AuthServerEntityStatementCreationStrategy;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jwk.PublicKeyUse;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.rise_world.gematik.accesskeeper.common.crypt.KeyConstants.PUK_IDP_ENC_SEK;
import static com.rise_world.gematik.accesskeeper.common.crypt.KeyConstants.PUK_IDP_SIG_SEK;

@Service
public class AuthServerEntityStatementServiceImpl implements AuthServerEntityStatementService {

    private final Clock clock;
    private final FederationMasterConfiguration config;
    private final ConfigService configService;
    private final AuthServerEntityStatementCreationStrategy tokenStrategy;
    private final KeyProvider keyProvider;
    private final SelfSignedCertificateService selfSignedCertificateService;

    @Autowired
    public AuthServerEntityStatementServiceImpl(KeyProvider keyProvider,
                                                Clock clock,
                                                FederationMasterConfiguration config,
                                                ConfigService configService,
                                                AuthServerEntityStatementCreationStrategy tokenStrategy,
                                                SelfSignedCertificateService selfSignedCertificateService) {
        this.clock = clock;
        this.config = config;
        this.configService = configService;
        this.tokenStrategy = tokenStrategy;
        this.keyProvider = keyProvider;
        this.selfSignedCertificateService = selfSignedCertificateService;
    }

    @Override
    // @AFO: A_23034 - das Entitystatement wird erstellt
    public String createEntityStatement() {
        Instant now = clock.instant();
        long epochSecond = now.getEpochSecond();
        String issuer = configService.getIssuer(RequestSource.INTERNET);
        Set<String> redirectUris = configService.getRedirectUrisForEntityStatement();

        // create claims
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(issuer);
        claims.setSubject(issuer);
        claims.setIssuedAt(epochSecond);
        // @AFO: A_23034 - die G&uuml;ltigkeit wird auf 24h gesetzt
        claims.setExpiryTime(now.plus(24, ChronoUnit.HOURS).getEpochSecond());
        claims.setClaim("jwks", new JsonWebKeys(createJsonWebKeyForSignature()));
        claims.setClaim("authority_hints", Collections.singletonList(config.getIssuer()));

        JwtClaims metadata = new JwtClaims();
        claims.setClaim("metadata", metadata);

        JwtClaims relyingPartyEntity = new JwtClaims();
        metadata.setClaim("openid_relying_party", relyingPartyEntity);
        List<JsonWebKey> keys = new ArrayList<>();
        keys.add(createJsonWebKeyForIdTokenEncryption());
        keys.addAll(selfSignedCertificateService.getValidKeys());
        relyingPartyEntity.setClaim("jwks", new JsonWebKeys(keys));
        relyingPartyEntity.setClaim("client_name", configService.getAuthServerClientName());
        relyingPartyEntity.setClaim("redirect_uris", redirectUris);
        relyingPartyEntity.setClaim("response_types", Collections.singletonList(OAuth2Constants.RESPONSE_TYPE_CODE));
        relyingPartyEntity.setClaim("client_registration_types", Collections.singletonList("automatic"));
        relyingPartyEntity.setClaim("grant_types", Collections.singletonList(OAuth2Constants.GRANT_TYPE_CODE));
        relyingPartyEntity.setClaim("require_pushed_authorization_requests", Boolean.TRUE);
        relyingPartyEntity.setClaim("token_endpoint_auth_method", "self_signed_tls_client_auth");
        // @AFO: A_23004 - default_acr_values wird gesetzt
        relyingPartyEntity.setClaim("default_acr_values", Collections.singletonList(OAuth2Constants.ACR_LOA_HIGH));
        relyingPartyEntity.setClaim("id_token_signed_response_alg", SignatureAlgorithm.ES256.getJwaName());
        relyingPartyEntity.setClaim("id_token_encrypted_response_alg", KeyAlgorithm.ECDH_ES_DIRECT.getJwaName());
        relyingPartyEntity.setClaim("id_token_encrypted_response_enc", ContentAlgorithm.A256GCM.getJwaName());
        relyingPartyEntity.setClaim("scope", "openid urn:telematik:display_name urn:telematik:versicherter");
        relyingPartyEntity.setClaim("organization_name", configService.getAuthServerOrganizationName());

        JwtClaims federationEntity = new JwtClaims();
        metadata.setClaim("federation_entity", federationEntity);
        federationEntity.setClaim("name",  configService.getAuthServerClientName());

        return tokenStrategy.toToken(claims);
    }

    private JsonWebKey createJsonWebKeyForSignature() {
        JsonWebKey jwk = JwkUtils.fromECPublicKey(keyProvider.getKey(PUK_IDP_SIG_SEK), JsonWebKey.EC_CURVE_P256);

        jwk.setKeyId(PUK_IDP_SIG_SEK);
        jwk.setPublicKeyUse(PublicKeyUse.SIGN);
        jwk.setAlgorithm(AlgorithmUtils.ES_SHA_256_ALGO);

        return jwk;
    }

    private JsonWebKey createJsonWebKeyForIdTokenEncryption() {
        JsonWebKey jwk = JwkUtils.fromECPublicKey(keyProvider.getKey(PUK_IDP_ENC_SEK), JsonWebKey.EC_CURVE_P256);

        jwk.setKeyId(PUK_IDP_ENC_SEK);
        jwk.setPublicKeyUse(PublicKeyUse.ENCRYPT);
        jwk.setAlgorithm(AlgorithmUtils.ECDH_ES_DIRECT_ALGO);

        return jwk;
    }

}

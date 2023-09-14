/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.extraction;

import com.rise_world.gematik.accesskeeper.common.OAuth2Constants;
import com.rise_world.gematik.accesskeeper.common.crypt.DecryptionProviderFactory;
import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.common.token.extraction.AbstractClaimExtractionStrategy;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.IssuedAtValidation;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.TokenExpiry;
import com.rise_world.gematik.accesskeeper.server.dto.EntityStatementDTO;
import com.rise_world.gematik.accesskeeper.server.dto.RequestSource;
import com.rise_world.gematik.accesskeeper.server.entity.ExtSessionEntity;
import com.rise_world.gematik.accesskeeper.server.service.ConfigService;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jwk.KeyType;
import org.apache.cxf.rs.security.jose.jws.EcDsaJwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.PublicKey;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import static com.rise_world.gematik.accesskeeper.common.token.ClaimUtils.KVNR_PATTERN;
import static com.rise_world.gematik.accesskeeper.common.token.ClaimUtils.MAX_LENGTH_DISPLAY_NAME;
import static com.rise_world.gematik.accesskeeper.common.token.ClaimUtils.MAX_LENGTH_NAME;

@Component
public class FederationIdTokenExtractionStrategy extends AbstractClaimExtractionStrategy {

    public static final String CONTEXT_ENTITY_STATEMENT = "remote";
    public static final String CONTEXT_SESSION = "session_content";

    private static final Logger LOG = LoggerFactory.getLogger(FederationIdTokenExtractionStrategy.class);

    private final ConfigService configService;

    @Autowired
    public FederationIdTokenExtractionStrategy(Clock clock,
                                               @Value("${token.iat.leeway}") long iatLeeway,
                                               DecryptionProviderFactory decryptionFactory,
                                               ConfigService configService) {
        // @AFO: A_23049 - Prüfen der zeitlichen Gültigkeit mittels TokenExpiry und IssuedAtValidation
        super(new FedIdpIdTokenParser(decryptionFactory.createDecryptionProvider(TokenType.SEKTORAL_ID_TOKEN)),
            new TokenExpiry(clock, ErrorCodes.FEDAUTH_INVALID_ID_TOKEN, ErrorCodes.FEDAUTH_INVALID_ID_TOKEN),
            new IssuedAtValidation(clock, ErrorCodes.FEDAUTH_INVALID_ID_TOKEN, iatLeeway));
        this.configService = configService;
    }

    @Override
    protected JwtClaims extractInternal(IdpJwsJwtCompactConsumer tokenConsumer, Map<String, Object> context) {
        EntityStatementDTO entityStatementDTO = (EntityStatementDTO) context.get(CONTEXT_ENTITY_STATEMENT);
        if (entityStatementDTO == null) {
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_MISSING_IDP_ES);
        }

        ExtSessionEntity session = (ExtSessionEntity) context.get(CONTEXT_SESSION);
        if (session == null) {
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_UNKNOWN_SESSION);
        }

        // @AFO: A_23049 - Prüfen der Tokensignatur
        validateSignature(tokenConsumer, entityStatementDTO);
        validateOidcClaims(tokenConsumer.getJwtClaims(), session, entityStatementDTO);
        validateGematikClaims(tokenConsumer.getJwtClaims());
        return tokenConsumer.getJwtClaims();
    }

    private void validateSignature(IdpJwsJwtCompactConsumer tokenConsumer, EntityStatementDTO entityStatementDTO) {
        if (StringUtils.isEmpty(tokenConsumer.getEncodedSignature())) {
            LOG.warn("id token signature is missing");
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_ID_TOKEN);
        }

        if (!SignatureAlgorithm.ES256.getJwaName().equals(tokenConsumer.getJwsHeaders().getAlgorithm())) {
            LOG.warn("signature algorithm {} is not supported", tokenConsumer.getJwsHeaders().getAlgorithm());
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_ID_TOKEN);
        }

        String keyId = tokenConsumer.getJwsHeaders().getKeyId();
        JsonWebKey key = entityStatementDTO.getKeys().get(keyId);

        if (key == null) {
            LOG.warn("Signature key is unknown [idp_iss={}] [kid={}]", entityStatementDTO.getIssuer(), keyId);
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_MISSING_SIG_KEY);
        }

        EcDsaJwsSignatureVerifier signatureVerifier = new EcDsaJwsSignatureVerifier(toPublicKey(key), SignatureAlgorithm.ES256);

        if (!tokenConsumer.verifySignatureWith(signatureVerifier)) {
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_ID_TOKEN);
        }
    }

    private PublicKey toPublicKey(JsonWebKey jwk) {
        if (jwk.getKeyType() != KeyType.EC) {
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_MISSING_SIG_KEY);
        }
        try {
            return JwkUtils.toECPublicKey(jwk);
        }
        catch (Exception e) {
            LOG.warn("Failed to parse JsonWebKey [type={}], [kid={}]", jwk.getProperty(JsonWebKey.KEY_TYPE), jwk.getKeyId());
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_MISSING_SIG_KEY);
        }
    }

    // @AFO: A_23049 - Validierung der OIDC Claims (aud, nonce)
    private void validateOidcClaims(JwtClaims idToken, ExtSessionEntity session, EntityStatementDTO entityStatementDTO) {
        String iss = idToken.getStringProperty(JwtConstants.CLAIM_ISSUER);
        if (!entityStatementDTO.getIssuer().equals(iss)) {
            LOG.warn("Issuer of id token does not match expected issuer [idp_iss={}] [iss_received={}]", entityStatementDTO.getIssuer(), iss);
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_ID_TOKEN);
        }

        // we don't allow multiple aud entries (our client_id is the only trusted aud)
        final List<String> audiences = idToken.getAudiences();
        if (audiences.size() != 1 || !configService.getIssuer(RequestSource.INTERNET).equals(audiences.get(0))) {
            LOG.warn("Audience of id token does not match expected value [idp_iss={}] [aud_received={}]", entityStatementDTO.getIssuer(), audiences);
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_ID_TOKEN);
        }

        final String nonce = idToken.getStringProperty("nonce");
        if (!session.getIdpNonce().equals(nonce)) {
            LOG.warn("nonce of id token does not match expected value [idp_iss={}]", entityStatementDTO.getIssuer());
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_ID_TOKEN);
        }

        final String acr = idToken.getStringProperty("acr");
        if (!OAuth2Constants.ACR_LOA_HIGH.equals(acr) && !OAuth2Constants.ACR_LOA_SUBSTENTIAL.equals(acr)) {
            LOG.warn("acr of id token does not match expected value [idp_iss={}] [acr_received={}]", entityStatementDTO.getIssuer(), acr);
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_ID_TOKEN);
        }
    }

    private void validateGematikClaims(JwtClaims idToken) {
        if (idToken.getClaim(ClaimUtils.URN_PROFESSION) == null ||
            idToken.getClaim(ClaimUtils.URN_DISPLAY_NAME) == null ||
            idToken.getClaim(ClaimUtils.URN_ID) == null ||
            idToken.getClaim(ClaimUtils.URN_ORGANIZATION) == null) {
            LOG.warn("token does not contain all required claims");
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_ID_TOKEN);
        }

        if (StringUtils.length(idToken.getStringProperty(ClaimUtils.URN_DISPLAY_NAME)) > MAX_LENGTH_DISPLAY_NAME) {
            LOG.warn("display_name is too long");
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_ID_TOKEN);
        }

        if (StringUtils.length(idToken.getStringProperty(ClaimUtils.URN_ORGANIZATION)) > MAX_LENGTH_NAME) {
            LOG.warn("organization_number is too long");
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_ID_TOKEN);
        }

        String idNumber = idToken.getStringProperty(ClaimUtils.URN_ID);
        if (StringUtils.length(idNumber) != 10 || !KVNR_PATTERN.matcher(idNumber).matches()) {
            LOG.warn("{} is too long or invalid", ClaimUtils.URN_ID);
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_ID_TOKEN);
        }

        // fed auth requires role 'versicherter'
        final String professionOid = idToken.getStringProperty(ClaimUtils.URN_PROFESSION);
        if (!ClaimUtils.PROFESSION_OID_VERSICHERTER.equals(professionOid)) {
            LOG.warn("invalid professionOid: {}", professionOid);
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_ID_TOKEN);
        }
    }
}

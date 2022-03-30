/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.extraction;

import com.rise_world.gematik.accesskeeper.common.OAuth2Constants;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.common.token.extraction.AbstractClaimExtractionStrategy;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.PlainTokenParser;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.IssuedAtValidation;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.TokenExpiry;
import com.rise_world.gematik.accesskeeper.server.dto.RemoteIdpDTO;
import com.rise_world.gematik.accesskeeper.server.entity.ExtSessionEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
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
import static com.rise_world.gematik.accesskeeper.common.token.ClaimUtils.MAX_LENGTH_NAME;

@Component
public class IdTokenExtractionStrategy extends AbstractClaimExtractionStrategy {

    public static final String CONTEXT_REMOTE_IDP = "remote";
    public static final String CONTEXT_SESSION = "session_content";

    private static final Logger LOG = LoggerFactory.getLogger(IdTokenExtractionStrategy.class);

    @Autowired
    public IdTokenExtractionStrategy(Clock clock, @Value("${token.iat.leeway}") long iatLeeway) {
        super(new PlainTokenParser(ErrorCodes.EXTAUTH_INVALID_ID_TOKEN),
                new TokenExpiry(clock, ErrorCodes.EXTAUTH_INVALID_ID_TOKEN, ErrorCodes.EXTAUTH_INVALID_ID_TOKEN),
                new IssuedAtValidation(clock, ErrorCodes.EXTAUTH_INVALID_ID_TOKEN, iatLeeway));
    }

    @Override
    // @AFO: A_22268 - ID-Token spezifische Validierungsschritte
    protected JwtClaims extractInternal(IdpJwsJwtCompactConsumer tokenConsumer, Map<String, Object> context) {
        RemoteIdpDTO remoteIdpDTO = (RemoteIdpDTO) context.get(CONTEXT_REMOTE_IDP);
        ExtSessionEntity session = (ExtSessionEntity) context.get(CONTEXT_SESSION);

        validateSignature(tokenConsumer, remoteIdpDTO);
        validateOidcClaims(tokenConsumer.getJwtClaims(), session, remoteIdpDTO);
        validateGematikClaims(tokenConsumer.getJwtClaims());
        return tokenConsumer.getJwtClaims();
    }

    // @AFO: A_22268 - Validierung der Tokensignatur
    private void validateSignature(IdpJwsJwtCompactConsumer tokenConsumer, RemoteIdpDTO remoteIdpDTO) {
        if (StringUtils.isEmpty(tokenConsumer.getEncodedSignature())) {
            LOG.warn("id token signature is missing");
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_INVALID_ID_TOKEN);
        }

        if (!SignatureAlgorithm.ES256.getJwaName().equals(tokenConsumer.getJwsHeaders().getAlgorithm())) {
            LOG.warn("signature algorithm {} is not supported", tokenConsumer.getJwsHeaders().getAlgorithm());
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_INVALID_ID_TOKEN);
        }

        String keyId = tokenConsumer.getJwsHeaders().getKeyId();
        PublicKey key = remoteIdpDTO.getWebKeys().get(keyId);

        if (key == null) {
            LOG.warn("Signature key is unknown [kk_app_id={}] [kid={}]", remoteIdpDTO.getAppConfig().getId(), keyId);
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_INVALID_ID_TOKEN);
        }

        EcDsaJwsSignatureVerifier signatureVerifier = new EcDsaJwsSignatureVerifier(key, SignatureAlgorithm.ES256);

        if (!tokenConsumer.verifySignatureWith(signatureVerifier)) {
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_INVALID_ID_TOKEN);
        }
    }

    // @AFO: A_22268 - Validierung der OIDC Claims
    private void validateOidcClaims(JwtClaims idToken, ExtSessionEntity session, RemoteIdpDTO remoteIdpDTO) {
        String iss = idToken.getStringProperty(JwtConstants.CLAIM_ISSUER);
        if (!remoteIdpDTO.getIssuer().equals(iss)) {
            LOG.warn("Issuer of id token does not match expected issuer [sek_idp_id={}] [iss_received={}]", remoteIdpDTO.getAppConfig().getId(), iss);
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_INVALID_ID_TOKEN);
        }

        // we don't allow multiple aud entries (our client_id is the only trusted aud)
        final List<String> audiences = idToken.getAudiences();
        if (audiences.size() != 1 || !OAuth2Constants.EXTERNAL_CLIENT_ID.equals(audiences.get(0))) {
            LOG.warn("Audience of id token does not match expected value [sek_idp_id={}] [aud_received={}]", remoteIdpDTO.getAppConfig().getId(), audiences);
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_INVALID_ID_TOKEN);
        }

        final String azp = idToken.getStringProperty("azp");
        if (azp != null && !OAuth2Constants.EXTERNAL_CLIENT_ID.equals(azp)) {
            LOG.warn("azp of id token is present but doesn't contain our client_id [sek_idp_id={}] [aud_received={}]", remoteIdpDTO.getAppConfig().getId(), azp);
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_INVALID_ID_TOKEN);
        }

        final String nonce = idToken.getStringProperty("nonce");
        if (!session.getIdpNonce().equals(nonce)) {
            LOG.warn("nonce of id token does not match expected value [sek_idp_id={}]", remoteIdpDTO.getAppConfig().getId());
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_INVALID_ID_TOKEN);
        }
    }

    private void validateGematikClaims(JwtClaims idToken) {
        // validate family_name, given_name and orgNumber are not null
        if (idToken.getClaim(ClaimUtils.FAMILY_NAME) == null ||
            idToken.getClaim(ClaimUtils.GIVEN_NAME) == null ||
            idToken.getClaim(ClaimUtils.SEK_IDP_ORG_NUMBER) == null) {
            LOG.warn("token does not contain all required claims");
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_INVALID_ID_TOKEN);
        }

        if (StringUtils.length(idToken.getStringProperty(ClaimUtils.FAMILY_NAME)) > MAX_LENGTH_NAME) {
            LOG.warn("family_name is too long");
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_INVALID_ID_TOKEN);
        }
        if (StringUtils.length(idToken.getStringProperty(ClaimUtils.GIVEN_NAME)) > MAX_LENGTH_NAME) {
            LOG.warn("given_name is too long");
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_INVALID_ID_TOKEN);
        }
        if (StringUtils.length(idToken.getStringProperty(ClaimUtils.SEK_IDP_ORG_NUMBER)) > MAX_LENGTH_NAME) {
            LOG.warn("organization_number is too long");
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_INVALID_ID_TOKEN);
        }

        String idNumber = idToken.getStringProperty(ClaimUtils.ID_NUMBER);
        if (StringUtils.length(idNumber) != 10 || !KVNR_PATTERN.matcher(idNumber).matches()) {
            LOG.warn("idNumber is too long or invalid");
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_INVALID_ID_TOKEN);
        }

        // fasttrack requires role 'versicherter'
        final String professionOid = idToken.getStringProperty(ClaimUtils.PROFESSION);
        if (!ClaimUtils.PROFESSION_OID_VERSICHERTER.equals(professionOid)) {
            LOG.warn("invalid professionOid: {}", professionOid);
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_INVALID_ID_TOKEN);
        }
    }
}

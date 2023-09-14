/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.extraction;

import com.rise_world.gematik.accesskeeper.common.crypt.CryptoConstants;
import com.rise_world.gematik.accesskeeper.common.crypt.DecryptionProviderFactory;
import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.common.token.extraction.ExtractionStrategy;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.JweTokenParser;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.EpkValidation;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.HeaderExpiry;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;

@Component
public class AuthenticationDataExtractionStrategy implements ExtractionStrategy<IdpJwsJwtCompactConsumer> {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationDataExtractionStrategy.class);

    private JweTokenParser tokenParser;

    @Autowired
    public AuthenticationDataExtractionStrategy(Clock clock, DecryptionProviderFactory decryptionFactory) {
        JweDecryptionProvider decryptionProvider = decryptionFactory.createDecryptionProvider(TokenType.ALTERNATIVE_AUTH_DATA);
        this.tokenParser = new JweTokenParser(decryptionProvider, ErrorCodes.VAL1_ALT_AUTH_FAILED,
            this::extractChallengeExp,
            new HeaderExpiry(clock, ErrorCodes.VAL1_ALT_AUTH_FAILED, ErrorCodes.VAL1_ALT_AUTH_FAILED),
            new EpkValidation(CryptoConstants.JWE_BRAINPOOL_CURVE, ErrorCodes.VAL1_ALT_AUTH_FAILED),
            h -> {
                if (!ClaimUtils.NESTED_TOKEN_CTY_VALUE.equals(h.getContentType()) || !ClaimUtils.hasJoseType(h, JoseType.JWT)) {
                    throw new AccessKeeperException(ErrorCodes.VAL1_ALT_AUTH_FAILED);
                }
            });
    }

    @Override
    public IdpJwsJwtCompactConsumer extractAndValidate(String token, Map<String, Object> context) {
        IdpJwsJwtCompactConsumer consumer = this.tokenParser.parse(token);
        validate(consumer);
        return consumer;
    }

    private void validate(IdpJwsJwtCompactConsumer consumer) {
        JwsHeaders jwsHeaders = consumer.getJwsHeaders();

        if (!SignatureAlgorithm.ES256.getJwaName().equals(jwsHeaders.getAlgorithm())) {
            LOG.warn("invalid header value algorithm");
            throw new AccessKeeperException(ErrorCodes.VAL1_ALT_AUTH_FAILED);
        }

        if (!ClaimUtils.hasJoseType(jwsHeaders, JoseType.JWT)) {
            LOG.warn("invalid header value type");
            throw new AccessKeeperException(ErrorCodes.VAL1_ALT_AUTH_FAILED);
        }
    }

    private Object extractChallengeExp(IdpJwsJwtCompactConsumer consumer) {
        final String challenge = consumer.getJwtClaims().getStringProperty(ClaimUtils.CHALLENGE_TOKEN);
        try {
            JwsJwtCompactConsumer challengeConsumer = new JwsJwtCompactConsumer(challenge);
            return challengeConsumer.getJwtClaims().getExpiryTime();
        }
        catch (Exception e) {
            // error handling is implemented in ChallengeExtractionStrategy
            return null;
        }
    }
}

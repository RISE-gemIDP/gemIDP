/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.extraction;

import com.rise_world.gematik.accesskeeper.common.crypt.KeyProvider;
import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.common.token.extraction.AbstractClaimExtractionStrategy;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.PlainTokenParser;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.IssuedAtValidation;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.ServerSignatureValidation;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.TokenExpiry;
import com.rise_world.gematik.accesskeeper.server.service.ConfigService;
import com.rise_world.gematik.accesskeeper.server.token.extraction.validation.ContentValidation;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;

@Component
@Qualifier("challenge")
public class ChallengeExtractionStrategy extends AbstractClaimExtractionStrategy {

    private static final String[] RELEVANT_CLAIMS = {ClaimUtils.SESSION_ID, ClaimUtils.CLIENT_ID, ClaimUtils.REDIRECT_URI,
        ClaimUtils.SCOPE, ClaimUtils.CODE_CHALLENGE_METHOD, ClaimUtils.CODE_CHALLENGE, ClaimUtils.RESPONSE_TYPE, ClaimUtils.STATE};

    @Autowired
    public ChallengeExtractionStrategy(Clock clock, ConfigService configService, KeyProvider keyProvider,
            @Value("${token.iat.leeway}") long iatLeeway) {
        super(
            new PlainTokenParser(ErrorCodes.AUTH_INVALID_CHALLENGE),
            new ServerSignatureValidation(keyProvider, ErrorCodes.AUTH_MISSING_SERVER_SIGNATURE, ErrorCodes.AUTH_WRONG_SERVER_ALGO, ErrorCodes.AUTH_INVALID_SERVER_SIGNATURE),
            // AFO: A_20314-01 - G&uuml;ltigkeit des Challenge-Tokens wird validiert
            new TokenExpiry(clock, ErrorCodes.AUTH_CHALLENGE_MISSING_EXPIRY, ErrorCodes.AUTH_CHALLENGE_EXPIRED),
            new IssuedAtValidation(clock, ErrorCodes.AUTH_INVALID_CHALLENGE, iatLeeway),
            new ContentValidation(configService, TokenType.CHALLENGE, RELEVANT_CLAIMS, ErrorCodes.AUTH_INVALID_CHALLENGE)
        );
    }

    @Override
    protected JwtClaims extractInternal(IdpJwsJwtCompactConsumer tokenConsumer, Map<String, Object> context) {
        return tokenConsumer.getJwtClaims();
    }
}

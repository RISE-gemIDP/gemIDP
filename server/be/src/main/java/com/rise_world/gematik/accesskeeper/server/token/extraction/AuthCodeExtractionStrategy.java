/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.extraction;

import com.rise_world.gematik.accesskeeper.common.crypt.DecryptionProviderFactory;
import com.rise_world.gematik.accesskeeper.common.crypt.KeyProvider;
import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.common.token.extraction.AbstractClaimExtractionStrategy;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.JweTokenParser;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.HeaderExpiry;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.IssuedAtValidation;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.ServerSignatureValidation;
import com.rise_world.gematik.accesskeeper.server.service.ConfigService;
import com.rise_world.gematik.accesskeeper.server.token.extraction.validation.AESValidation;
import com.rise_world.gematik.accesskeeper.server.token.extraction.validation.ContentValidation;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;

@Component
@Qualifier("authorizationCode")
public class AuthCodeExtractionStrategy extends AbstractClaimExtractionStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(AuthCodeExtractionStrategy.class);

    private static final String[] RELEVANT_CLAIMS = {ClaimUtils.AUTH_TIME, ClaimUtils.SCOPE, ClaimUtils.CLIENT_ID, ClaimUtils.ID_NUMBER};

    @Autowired
    public AuthCodeExtractionStrategy(Clock clock, KeyProvider keyProvider, DecryptionProviderFactory decryptionFactory, ConfigService configService,
            @Value("${token.iat.leeway}") long iatLeeway) {
        // @AFO: A_20321 - Entschlüsselung des Authorization Code
        super(new JweTokenParser(decryptionFactory.createDecryptionProvider(TokenType.AUTH_CODE),
                ErrorCodes.TOKEN_INVALID_AUTH_CODE,
                // @AFO: A_20315-01 - "AUTHORIZATION_CODE" nach G&uuml;ltigkeitsende nicht mehr verwenden
                new HeaderExpiry(clock, ErrorCodes.TOKEN_MISSING_EXPIRY, ErrorCodes.TOKEN_AUTH_CODE_EXPIRED),
                new AESValidation(ErrorCodes.TOKEN_INVALID_AUTH_CODE)),
            // @AFO: A_21318 - Prüfung der Signatur des Authorization Codes
            new ServerSignatureValidation(keyProvider, ErrorCodes.TOKEN_BROKEN_SIGNATURE),
            new IssuedAtValidation(clock, ErrorCodes.TOKEN_INVALID_AUTH_CODE, iatLeeway),
            new ContentValidation(configService, TokenType.AUTH_CODE, new String[]{}, ErrorCodes.TOKEN_INVALID_AUTH_CODE)
            );
    }

    @Override
    protected JwtClaims extractInternal(IdpJwsJwtCompactConsumer tokenConsumer, Map<String, Object> context) {
        JwtClaims claims = tokenConsumer.getJwtClaims();

        if (!ClaimUtils.containsAllClaims(claims, RELEVANT_CLAIMS)) {
            LOG.error("authorization code is missing relevant claims");
            throw new AccessKeeperException(ErrorCodes.TOKEN_MISSING_CLAIMS);
        }

        return claims;
    }
}


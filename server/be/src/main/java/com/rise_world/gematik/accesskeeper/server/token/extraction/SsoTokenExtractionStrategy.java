/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.extraction;

import com.rise_world.gematik.accesskeeper.common.crypt.DecryptionProviderFactory;
import com.rise_world.gematik.accesskeeper.common.crypt.KeyProvider;
import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@Qualifier("ssoToken")
public class SsoTokenExtractionStrategy extends AbstractClaimExtractionStrategy {

    private static final String[] RELEVANT_CLAIMS = {ClaimUtils.CLIENT_ID, ClaimUtils.REDIRECT_URI, ClaimUtils.AUTH_TIME, ClaimUtils.CERTIFICATE};

    @Autowired
    // @AFO: A_20949 - Wirft eine Exception mit ErrorCode AUTH_INVALID_SSO_TOKEN wenn der SSO Token nicht verarbeitet werden kann
    // @AFO: A_20947 - Entschl&uuml;sselung des "SSO_TOKEN"
    public SsoTokenExtractionStrategy(Clock clock, DecryptionProviderFactory provider, ConfigService configService, KeyProvider keyProvider) {
        super(
            // Entschluesselung via JweTokenParser
            new JweTokenParser(provider.createDecryptionProvider(TokenType.SSO),
                ErrorCodes.AUTH_INVALID_SSO_TOKEN,
                new HeaderExpiry(clock, ErrorCodes.AUTH_INVALID_SSO_TOKEN, ErrorCodes.AUTH_INVALID_SSO_TOKEN),
                new AESValidation(ErrorCodes.AUTH_INVALID_SSO_TOKEN)),
            // @AFO: A_20948-01 - Die Signatur des SSO Tokens wird überprüft
            new ServerSignatureValidation(keyProvider, ErrorCodes.AUTH_INVALID_SSO_TOKEN),
            new IssuedAtValidation(clock, ErrorCodes.AUTH_INVALID_SSO_TOKEN),
            new ContentValidation(configService, TokenType.SSO, RELEVANT_CLAIMS, ErrorCodes.AUTH_INVALID_SSO_TOKEN)
        );
    }

    @Override
    protected JwtClaims extractInternal(IdpJwsJwtCompactConsumer tokenConsumer) {
        return tokenConsumer.getJwtClaims();
    }
}

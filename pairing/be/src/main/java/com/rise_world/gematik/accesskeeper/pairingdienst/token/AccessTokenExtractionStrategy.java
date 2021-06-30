/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.token;

import com.rise_world.gematik.accesskeeper.common.OAuth2Constants;
import com.rise_world.gematik.accesskeeper.common.crypt.DecryptionProviderFactory;
import com.rise_world.gematik.accesskeeper.common.crypt.KeyProvider;
import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.common.token.extraction.AbstractClaimExtractionStrategy;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.JweTokenParser;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.EpkValidation;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.HeaderExpiry;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.IssuedAtValidation;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.ServerSignatureValidation;
import com.rise_world.gematik.accesskeeper.pairingdienst.service.AccessTokenParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes.REG1_CLIENT_ERROR;

@Component
@Qualifier("accessToken")
public class AccessTokenExtractionStrategy extends AbstractClaimExtractionStrategy implements AccessTokenParser {

    public static final String SCOPE_PAIRING = "pairing";

    private static final Logger LOG = LoggerFactory.getLogger(AccessTokenExtractionStrategy.class);

    // @AFO: A_21413 - erlaubte Scopes
    private static final List<String> EXPECTED_SCOPES = Arrays.asList(OAuth2Constants.SCOPE_OPENID, SCOPE_PAIRING);

    private static final Set<String> EXPECTED_CLAIMS = new HashSet<>(Arrays.asList(ClaimUtils.ID_NUMBER, ClaimUtils.AUTH_METHOD,
            ClaimUtils.AUTH_CTX, ClaimUtils.AUTH_TIME, ClaimUtils.SCOPE, ClaimUtils.CLIENT_ID, ClaimUtils.AUTH_PARTY,
            JwtConstants.CLAIM_AUDIENCE, JwtConstants.CLAIM_SUBJECT, JwtConstants.CLAIM_ISSUER,
            JwtConstants.CLAIM_ISSUED_AT, JwtConstants.CLAIM_EXPIRY, JwtConstants.CLAIM_JWT_ID));

    @Autowired
    public AccessTokenExtractionStrategy(Clock clock, DecryptionProviderFactory decryptionFactory, KeyProvider keyProvider) {
        super(new JweTokenParser(decryptionFactory.createDecryptionProvider(TokenType.ACCESS),
                REG1_CLIENT_ERROR, // @AFO: A_21411 Fehlerfall: Entschlüsselung nicht möglich
                // @AFO: A_20372 Access Token wird auf gültiges EXP Feld geprüft
                // @AFO: A_21411 Fehlerfall: ungültiger EXP
                new HeaderExpiry(clock, REG1_CLIENT_ERROR, REG1_CLIENT_ERROR),
                new EpkValidation(REG1_CLIENT_ERROR)), // @AFO: A_21411 Fehlerfall: ungültiger EPK Header
            // @AFO: A_20372 Access Token wird auf gültiges IAT Feld geprüft
            // @AFO: A_21411 Fehlerfall: ungültiger IAT
            new IssuedAtValidation(clock, REG1_CLIENT_ERROR), // @AFO: A_21411 Fehlerfall: ungültiger IAT
            // @AFO: A_20365 Prüfung der Signatur gegen den öffentlichen Schlüssel des IdP
            // @AFO: A_21411 Fehlerfall: ungültige IdP Signatur
            new ServerSignatureValidation(keyProvider, REG1_CLIENT_ERROR));
    }

    @Override
    protected JwtClaims extractInternal(IdpJwsJwtCompactConsumer consumer) {
        JwtClaims claims = consumer.getJwtClaims();

        validateContent(claims);

        return claims;
    }

    private void validateContent(JwtClaims claims) {

        if (!EXPECTED_CLAIMS.equals(claims.asMap().keySet())) {
            LOG.error("invalid access token: wrong claims");
            // @AFO: A_21411 Fehlerfall: unerwartete oder fehlende Claims
            throw new AccessKeeperException(REG1_CLIENT_ERROR);
        }

        // @AFO: A_21422 - idNummer muss im Token enthalten sein
        if (StringUtils.isBlank(claims.getStringProperty(ClaimUtils.ID_NUMBER))) {
            LOG.error("invalid access token: invalid idNummer claim");
            // @AFO: A_21411 Fehlerfall: leere idNummer
            throw new AccessKeeperException(REG1_CLIENT_ERROR);
        }

        // @AFO: A_21419 - ACR Prüfung auf gematik-ehealth-loa-high
        String acr = claims.getStringProperty(ClaimUtils.AUTH_CTX);
        if (!OAuth2Constants.ACR_LOA_HIGH.equals(acr)) {
            LOG.error("invalid access token: invalid Authentication Context Class Reference");
            // @AFO: A_21411 Fehlerfall: falscher ACR Wert
            throw new AccessKeeperException(REG1_CLIENT_ERROR);
        }


        List<String> methods = claims.getListStringProperty(ClaimUtils.AUTH_METHOD);
        if (CollectionUtils.isEmpty(methods)) {
            LOG.warn("invalid access token: invalid Authentication Method References");
            throw new AccessKeeperException(REG1_CLIENT_ERROR);
        }

        // @AFO: A_21413 - Der Scope des AccessTokens muss 'openid pairing' sein
        // @AFO: A_21442 - Der Scope des AccessTokens muss 'openid pairing' sein
        List<String> scopes = ClaimUtils.getScopes(claims);
        if (CollectionUtils.isEmpty(scopes) || scopes.size() != EXPECTED_SCOPES.size() || !scopes.containsAll(EXPECTED_SCOPES)) {
            LOG.warn("invalid access token: Scope contains unexpected values ");
            // @AFO: A_21411 Fehlerfall: unerwartete oder fehlende Scopes
            throw new AccessKeeperException(REG1_CLIENT_ERROR);
        }
    }

}

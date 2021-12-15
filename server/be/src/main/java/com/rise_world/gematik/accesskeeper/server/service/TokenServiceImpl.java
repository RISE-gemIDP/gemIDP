/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.common.OAuth2Constants;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.common.token.extraction.ClaimExtractionStrategy;
import com.rise_world.gematik.accesskeeper.common.token.extraction.ExtractionStrategy;
import com.rise_world.gematik.accesskeeper.common.util.DigestUtils;
import com.rise_world.gematik.accesskeeper.common.util.LangUtils;
import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import com.rise_world.gematik.accesskeeper.common.util.RandomUtils;
import com.rise_world.gematik.accesskeeper.server.dto.RedeemedTokenDTO;
import com.rise_world.gematik.accesskeeper.server.model.Client;
import com.rise_world.gematik.accesskeeper.server.model.Fachdienst;
import com.rise_world.gematik.accesskeeper.server.token.creation.AesTokenCreationStrategy;
import com.rise_world.gematik.accesskeeper.server.token.extraction.KeyVerifier;
import com.rise_world.gematik.accesskeeper.server.util.PkceUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Service
public class TokenServiceImpl implements TokenService {

    private static final Logger LOG = LoggerFactory.getLogger(TokenServiceImpl.class);

    // @AFO: A_20313-01 - Inhalte des Claims
    // @AFO: A_20463 - Maximale Gültigkeitsdauer des "ACCESS_TOKEN"
    private static final long MAX_AT_EXPIRATION = 300;

    // @AFO: A_20462 - Maximale Gültigkeitsdauer des "ID_TOKEN"
    // @AFO: A_20313-01 - Inhalte des Claims
    private static final long MAX_ID_EXPIRATION = 86400;

    // relevant authorization code claims for access token
    private static final String[] RELEVANT_CLAIMS_FOR_AT = {
        ClaimUtils.AUTH_TIME,
        ClaimUtils.SCOPE,
        ClaimUtils.CLIENT_ID,
        ClaimUtils.GIVEN_NAME,
        ClaimUtils.FAMILY_NAME,
        ClaimUtils.ORG_NAME,
        ClaimUtils.PROFESSION,
        ClaimUtils.ID_NUMBER
    };

    // relevant authorization code claims for id token
    private static final String[] RELEVANT_CLAIMS_FOR_ID = {
        ClaimUtils.AUTH_TIME,
        ClaimUtils.NONCE,
        ClaimUtils.GIVEN_NAME,
        ClaimUtils.FAMILY_NAME,
        ClaimUtils.ORG_NAME,
        ClaimUtils.PROFESSION,
        ClaimUtils.ID_NUMBER
    };

    private ClaimExtractionStrategy authCodeExtraction;
    private ExtractionStrategy<KeyVerifier> keyVerifierExtraction;
    private ConfigService config;
    private AesTokenCreationStrategy accStrategy;
    private AesTokenCreationStrategy idStrategy;
    private Clock clock;


    @Autowired
    public TokenServiceImpl(
            @Qualifier("accessTokenStrategyFactory") AesTokenCreationStrategy accStrategy,
            @Qualifier("idTokenStrategyFactory") AesTokenCreationStrategy idStrategy,
            @Qualifier("authorizationCode") ClaimExtractionStrategy authCodeExtraction,
            @Qualifier("keyVerifier") ExtractionStrategy<KeyVerifier> keyVerifierExtraction,
            ConfigService config,
            Clock clock) {
        this.accStrategy = accStrategy;
        this.idStrategy = idStrategy;
        this.authCodeExtraction = authCodeExtraction;
        this.keyVerifierExtraction = keyVerifierExtraction;
        this.config = config;
        this.clock = clock;
    }

    @Override
    public RedeemedTokenDTO redeemToken(String authCode, String keyVerifier, String clientId, String grantType, String redirectUri) {

        verifyIntegrity(authCode, keyVerifier, clientId, grantType, redirectUri);

        // extract tokens
        JwtClaims authCodeClaims = authCodeExtraction.extractAndValidate(authCode);
        KeyVerifier keyVerifierClaims = keyVerifierExtraction.extractAndValidate(keyVerifier);

        try {
            // apply session id
            LogTool.setSessionId(authCodeClaims.getStringProperty(ClaimUtils.SESSION_ID));

            List<String> scopes = ClaimUtils.getScopes(authCodeClaims);
            Fachdienst fachdienst = this.getFachdienst(scopes);

            String codeVerifier = keyVerifierClaims.getCodeVerifier();
            SecretKey tokenKey = keyVerifierClaims.getSecretTokenKey();

            verifyParameters(codeVerifier, clientId, grantType, redirectUri, authCodeClaims);

            String accToken = createAccessToken(authCodeClaims, tokenKey, fachdienst);

            RedeemedTokenDTO tokens = new RedeemedTokenDTO();
            tokens.setAccessToken(accToken);
            tokens.setIdToken(createIDToken(authCodeClaims, accToken, tokenKey, fachdienst));
            tokens.setExpires(getExpiryTime(fachdienst::getTokenTimeout, MAX_AT_EXPIRATION));
            return tokens;
        }
        finally {
            keyVerifierClaims.destroy();
        }
    }

    private void verifyIntegrity(String authCode, String keyVerifier, String clientId, String grantType, String redirectUri) {
        if (LangUtils.isBlankOrEmpty(authCode)) {
            LOG.warn("authorization code is missing");
            throw new AccessKeeperException(ErrorCodes.TOKEN_MISSING_AUTH_CODE);
        }
        if (LangUtils.isBlankOrEmpty(keyVerifier)) {
            LOG.warn("key_verifier is missing");
            throw new AccessKeeperException(ErrorCodes.TOKEN_MISSING_KEY_VERIFIER);
        }
        if (LangUtils.isBlankOrEmpty(clientId)) {
            LOG.warn("client_id is missing");
            throw new AccessKeeperException(ErrorCodes.COMMON_MISSING_CLIENT_ID);
        }
        if (LangUtils.isBlankOrEmpty(grantType)) {
            LOG.warn("grant_type is missing");
            throw new AccessKeeperException(ErrorCodes.TOKEN_MISSING_GRANT);
        }
        if (LangUtils.isBlankOrEmpty(redirectUri)) {
            LOG.warn("redirect_uri is missing");
            throw new AccessKeeperException(ErrorCodes.COMMON_MISSING_REDIRECT);
        }
    }

    private void verifyParameters(String codeVerifier, String clientId, String grantType, String redirectUri, JwtClaims authCodeClaims) {

        if (!OAuth2Constants.GRANT_TYPE_CODE.equals(grantType)) {
            LOG.warn("grant_type {} is not supported", grantType);
            throw new AccessKeeperException(ErrorCodes.TOKEN_UNSUPPORTED_GRANT);
        }

        // @AFO: A_21319 Prüfung des entschlüsselten code_verifier gegen die code_challenge unter Nutzung der code_challenge_methode S256
        if (!OAuth2Constants.PKCE_METHOD_S256.equals(authCodeClaims.getStringProperty(ClaimUtils.CODE_CHALLENGE_METHOD)) ||
            !verifyCodeChallenge(codeVerifier, authCodeClaims.getStringProperty(ClaimUtils.CODE_CHALLENGE))) {
            LOG.warn("code_challenge does not match code_verifier");
            throw new AccessKeeperException(ErrorCodes.TOKEN_BROKEN_CODE_CHALLENGE);
        }

        if (config.getClientById(clientId) == null ||
            !Objects.equals(clientId, authCodeClaims.getStringProperty(ClaimUtils.CLIENT_ID))) {
            LOG.warn("client_id does not match client_id of authoriziation code");
            throw new AccessKeeperException(ErrorCodes.TOKEN_INVALID_CLIENT);
        }

        if (!Objects.equals(redirectUri, authCodeClaims.getStringProperty(ClaimUtils.REDIRECT_URI))) {
            LOG.warn("redirect_uri does not match redirect_uri of authorization code");
            throw new AccessKeeperException(ErrorCodes.COMMON_INVALID_REDIRECT_URI);
        }
    }

    private boolean verifyCodeChallenge(String codeVerifier, String codeChallenge) {
        return codeChallenge.equals(PkceUtils.createCodeChallenge(codeVerifier));
    }

    private Fachdienst getFachdienst(List<String> scopes) {
        boolean hasScopeOID = scopes.remove(OAuth2Constants.SCOPE_OPENID);

        if (!hasScopeOID || scopes.size() != 1) {
            LOG.warn("wrong number of scopes");
            throw new AccessKeeperException(ErrorCodes.COMMON_INVALID_SCOPE);
        }

        String scope = scopes.get(0);
        Fachdienst fd = config.getFachdienstByScope(scope);

        if (fd == null) {
            LOG.warn("scope {} is not supported", scope);
            throw new AccessKeeperException(ErrorCodes.COMMON_UNKNOWN_FD);
        }

        return fd;
    }

    /**
     * Creates an Access Token based on the provided Authorization Code claims
     *
     * @param authCode   Authorization Code claims
     * @param key        encryption key
     * @param fachdienst relevant Fachdienst
     * @return string representation of an Access Token
     * @AFO: A_20464 - Token-Endpunkt (Datensparsamkeit)
     * @AFO: A_20459 - Das Attribut AUTH_TIME muss in allen Token unverändert bleiben
     * @AFO: A_20524-02 - Befüllen der Claims "given_name", "family_name", "organizationName", "professionOID"
     *       und "idNummer"
     */
    protected String createAccessToken(JwtClaims authCode, SecretKey key, Fachdienst fachdienst) {
        JwtClaims relevantClaims = ClaimUtils.filter(authCode, RELEVANT_CLAIMS_FOR_AT);
        String clientId = authCode.getStringProperty(ClaimUtils.CLIENT_ID);
        String idNumber = StringUtils.defaultString(authCode.getStringProperty(ClaimUtils.ID_NUMBER));

        relevantClaims.setClaim(ClaimUtils.AUTH_PARTY, clientId);
        // @AFO: A_20524-02 Befüllen der Claims acr und amr
        relevantClaims.setClaim(ClaimUtils.AUTH_CTX, OAuth2Constants.ACR_LOA_HIGH);
        relevantClaims.setClaim(ClaimUtils.AUTH_METHOD, authCode.getListStringProperty(ClaimUtils.AUTH_METHOD));
        // @AFO: A_20952 - Claim "aud" im Token setzen
        relevantClaims.setAudience(fachdienst.getAud());
        relevantClaims.setSubject(createSubject(fachdienst, idNumber, config.getSalt()));
        long iat = clock.instant().getEpochSecond();

        relevantClaims.setIssuer(config.getIssuer(RequestContext.getRequestSource()));
        relevantClaims.setIssuedAt(iat);
        // @AFO: A_20313-01 - Inhalte des Claims
        relevantClaims.setExpiryTime(calculateAccessTokenExpiryTime(fachdienst, iat));
        relevantClaims.setTokenId(RandomUtils.randomUUID());

        // @AFO: A_20327-02 - Signatur des "ACCESS_TOKEN"
        // @AFO: A_21321 - Verschlüsselung des "ACCESS_TOKEN"
        return accStrategy.toToken(relevantClaims, key);
    }


    /**
     * Creates an ID Token based on the provided Authorization Code claims
     *
     * @param authCodeClaims Authorization Code claims
     * @param accessToken    string representation of an Access Token
     * @param key            encryption key
     * @param fachdienst     relevant Fachdienst
     * @return string representation of an ID Token
     * @AFO: A_20459 - Das Attribut AUTH_TIME muss in allen Token unverändert bleiben
     * @AFO: A_20464 - Token-Endpunkt (Datensparsamkeit)
     * @AFO: A_20524-02  - Befüllen der Claims "given_name", "family_name", "organizationName", "professionOID"
     *       und "idNummer"
     */
    protected String createIDToken(JwtClaims authCodeClaims, String accessToken, SecretKey key, Fachdienst fachdienst) {
        JwtClaims relevantClaims = ClaimUtils.filter(authCodeClaims, RELEVANT_CLAIMS_FOR_ID);
        String clientId = authCodeClaims.getStringProperty(ClaimUtils.CLIENT_ID);
        String kvnr = StringUtils.defaultString(authCodeClaims.getStringProperty(ClaimUtils.ID_NUMBER));

        relevantClaims.setClaim(ClaimUtils.AUTH_PARTY, clientId);
        // @AFO: A_20524-02 Befüllen der Claims acr und amr
        relevantClaims.setClaim(ClaimUtils.AUTH_CTX, OAuth2Constants.ACR_LOA_HIGH);
        relevantClaims.setClaim(ClaimUtils.AUTH_METHOD, authCodeClaims.getListStringProperty(ClaimUtils.AUTH_METHOD));
        relevantClaims.setAudience(clientId);
        relevantClaims.setSubject(createSubject(fachdienst, kvnr, config.getSalt()));

        long iat = clock.instant().getEpochSecond();

        relevantClaims.setIssuer(config.getIssuer(RequestContext.getRequestSource()));
        relevantClaims.setIssuedAt(iat);
        // @AFO: A_20462 - Maximale Gültigkeitsdauer des "ID_TOKEN"
        // @AFO: A_20313-01 - Inhalte des Claims
        relevantClaims.setExpiryTime(calculateIdTokenExpiryTime(config.getClientById(clientId), iat));
        relevantClaims.setTokenId(RandomUtils.randomUUID());
        relevantClaims.setClaim(ClaimUtils.AT_HASH, createHash(accessToken));

        // @AFO: A_20327-02 - Signatur des "ID_TOKEN"
        // @AFO: A_21321 - Verschlüsselung des "ID_TOKEN"
        return this.idStrategy.toToken(relevantClaims, key);
    }

    /**
     * Calculates the expiry time for an access token.
     *
     * @param fachdienst    configuration for expiry time
     * @param iat           issuer date
     * @return the calculated expiry date
     *
     * @AFO: A_20313-01 - Inhalte des Claims (gemProdT_IDP-Dienst)
     * @AFO: A_20463 - Maximale Gültigkeitsdauer des "ACCESS_TOKEN" (gemProdT_IDP-Dienst)
     */
    private long calculateAccessTokenExpiryTime(Fachdienst fachdienst, long iat) {
        return iat + getExpiryTime(fachdienst::getTokenTimeout, MAX_AT_EXPIRATION);
    }

    /**
     * Calculates expiry time depending on the timeout provider and the upper bound value. The expiry time must
     * not exceed the upper bound value. If the provided timeout is null, below zero or above the upper bound
     * value, the upper bound value will be returned.
     *
     * @param timeoutProvider   provides timeout configuration
     * @param upperBound        represents the upper bound and default value
     * @return expiry time calculated using the provider and the upper bound value
     */
    @SuppressWarnings("squid:S4276") // we need to check against Long objects
    private long getExpiryTime(Supplier<Long> timeoutProvider, long upperBound) {
        Long timeout = timeoutProvider.get();
        // if timeout is below zero or above max. expiry time, set to max. expiry time
        if (timeout == null || timeout < 0 || timeout > upperBound) {
            return upperBound;
        }
        return timeout;
    }

    private Object createHash(String accessToken) {
        byte[] hash = DigestUtils.sha256(accessToken.getBytes());

        return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(hash, 16));
    }

    /**
     * Calculates expiry time depending on the configuration of the addressed Fachdienst. The expiry time must
     * not exceed 300 seconds
     *
     * @param iat time token has been issued
     * @return expiry time calculated using configuration and iat
     * @AFO: A_20313-01 - Inhalte des Claims
     * @AFO: A_20462 - Maximale Gültigkeitsdauer des "ID_TOKEN"
     */
    private long calculateIdTokenExpiryTime(Client client, long iat) {
        return iat + getExpiryTime(client::getTokenTimeout, MAX_ID_EXPIRATION);
    }


    private String createSubject(Fachdienst fd, String identifier, String salt) {
        Validate.isTrue(identifier != null);

        String subject = fd.getSectorIdentifier() + identifier + salt;
        return DigestUtils.sha256Hex(subject, StandardCharsets.UTF_8);
    }
}


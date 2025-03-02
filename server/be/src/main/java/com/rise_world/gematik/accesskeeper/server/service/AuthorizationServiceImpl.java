/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.common.OAuth2Constants;
import com.rise_world.gematik.accesskeeper.common.api.pairing.AuthenticationResponse;
import com.rise_world.gematik.accesskeeper.common.api.pairing.PairingVerificationInternalEndpoint;
import com.rise_world.gematik.accesskeeper.common.api.pairing.SignedAuthenticationDataRequest;
import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.CertReaderException;
import com.rise_world.gematik.accesskeeper.common.exception.CertificateServiceException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.service.CertificateReaderService;
import com.rise_world.gematik.accesskeeper.common.service.CertificateServiceClient;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.common.token.creation.TokenCreationStrategy;
import com.rise_world.gematik.accesskeeper.common.token.extraction.ClaimExtractionStrategy;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import com.rise_world.gematik.accesskeeper.common.util.RandomUtils;
import com.rise_world.gematik.accesskeeper.common.util.UrlBuilder;
import com.rise_world.gematik.accesskeeper.server.dto.ChallengeDTO;
import com.rise_world.gematik.accesskeeper.server.dto.EntityStatementDTO;
import com.rise_world.gematik.accesskeeper.server.dto.RedeemedChallengeDTO;
import com.rise_world.gematik.accesskeeper.server.dto.RedeemedSsoTokenDTO;
import com.rise_world.gematik.accesskeeper.server.dto.RequestSource;
import com.rise_world.gematik.accesskeeper.server.dto.UserConsentDTO;
import com.rise_world.gematik.accesskeeper.server.entity.ExtSessionEntity;
import com.rise_world.gematik.accesskeeper.server.model.Client;
import com.rise_world.gematik.accesskeeper.server.model.Fachdienst;
import com.rise_world.gematik.accesskeeper.server.model.Scope;
import com.rise_world.gematik.accesskeeper.server.token.extraction.AuthenticationDataExtractionStrategy;
import com.rise_world.gematik.accesskeeper.server.token.extraction.FederationIdTokenExtractionStrategy;
import com.rise_world.gematik.accesskeeper.server.util.LoopbackUtils;
import com.rise_world.gematik.accesskeeper.server.util.PkceUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@Component
public class AuthorizationServiceImpl implements AuthorizationService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationServiceImpl.class);

    private static final int MAX_LENGTH_STATE = 512;
    private static final int MAX_LENGTH_NONCE = 512;
    private static final int MAX_LENGTH_CODE_CHALLENGE = 43;
    private static final int MAX_LENGTH_REDIRECT_URI = 2000;
    private static final int MAX_LENGTH_IDP_ISS = 2000;
    private static final int MAX_LENGTH_AUTH_CODE_SEK = 2000;
    private static final int MAX_LENGTH_AMR = 40;

    private static final int MAX_AMR_VALUES = 8;

    // @AFO: A_20314-01 -  Konstante max. G&uuml;ltigkeit des Challenge-Tokens (180s)
    private static final long MAX_CHALLENGE_EXPIRATION = TimeUnit.MINUTES.toSeconds(3);

    // @AFO: A_20692-01 - Konstante max. SSO Token G&uuml;ltigkeit
    private static final long MAX_SSO_EXPIRATION = TimeUnit.HOURS.toSeconds(24L);

    // @AFO: A_20314-01 - Konstante max. AUTH_CODE G&uuml;ltigkeit (60s)
    private static final long MAX_AUTH_CODE_EXPIRATION = 60;

    private static final List<String> AMR_METHODS = Arrays.asList(OAuth2Constants.AMR_MULTI_FACTOR_AUTH, OAuth2Constants.AMR_SMART_CARD, OAuth2Constants.AMR_PIN);

    private ConfigService configService;
    private ClaimExtractionStrategy signedChallengeExtractionStrategy;
    private ClaimExtractionStrategy challengeExtractionStrategy;
    private ClaimExtractionStrategy ssoTokenExtractionStrategy;
    private ClaimExtractionStrategy federationIdTokenExtractionStrategy;
    private AuthenticationDataExtractionStrategy authenticationDataExtractionStrategy;
    private TokenCreationStrategy challengeStrategy;
    private TokenCreationStrategy authCodeStrategy;
    private TokenCreationStrategy ssoStrategy;
    private Clock clock;
    private CertificateReaderService certReader;
    private CertificateServiceClient certificateServiceClient;
    private PairingVerificationInternalEndpoint pairingDienstClient;
    private SessionStorage sessionStorage;
    private ExtParService extParService;
    private ExtAuthCodeService extAuthCodeService;
    private TaskExecutor taskExecutor;
    private EntityStatementSynchronizationService entityStatementSynchronizationService;


    @Autowired
    @SuppressWarnings("squid:S00107") // parameters required for dependency injection
    // CHECKSTYLE:OFF More than 10 parameters
    public AuthorizationServiceImpl(ConfigService configService,
                                    @Qualifier("challengeStrategy") TokenCreationStrategy challengeStrategy,
                                    @Qualifier("authCodeStrategy") TokenCreationStrategy authCodeStrategy,
                                    @Qualifier("ssoTokenStrategy") TokenCreationStrategy ssoStrategy,
                                    @Qualifier("signedChallenge") ClaimExtractionStrategy signedChallengeExtractionStrategy,
                                    @Qualifier("challenge") ClaimExtractionStrategy challengeExtractionStrategy,
                                    @Qualifier("ssoToken") ClaimExtractionStrategy ssoTokenExtractionStrategy,
                                    FederationIdTokenExtractionStrategy federationIdTokenExtractionStrategy,
                                    AuthenticationDataExtractionStrategy authenticationDataExtractionStrategy,
                                    Clock clock,
                                    CertificateReaderService certReader,
                                    CertificateServiceClient certificateServiceClient,
                                    PairingVerificationInternalEndpoint pairingDienstClient,
                                    SessionStorage sessionStorage,
                                    ExtParService extParService,
                                    ExtAuthCodeService extAuthCodeService,
                                    TaskExecutor taskExecutor,
                                    EntityStatementSynchronizationService entityStatementSynchronizationService) {
        // CHECKSTYLE:ON
        this.configService = configService;

        this.challengeStrategy = challengeStrategy;
        this.authCodeStrategy = authCodeStrategy;
        this.ssoStrategy = ssoStrategy;

        this.signedChallengeExtractionStrategy = signedChallengeExtractionStrategy;
        this.challengeExtractionStrategy = challengeExtractionStrategy;
        this.ssoTokenExtractionStrategy = ssoTokenExtractionStrategy;
        this.authenticationDataExtractionStrategy = authenticationDataExtractionStrategy;
        this.federationIdTokenExtractionStrategy = federationIdTokenExtractionStrategy;

        this.clock = clock;
        this.certReader = certReader;
        this.certificateServiceClient = certificateServiceClient;
        this.pairingDienstClient = pairingDienstClient;
        this.sessionStorage = sessionStorage;
        this.extParService = extParService;
        this.extAuthCodeService = extAuthCodeService;

        this.taskExecutor = taskExecutor;
        this.entityStatementSynchronizationService = entityStatementSynchronizationService;
    }

    @Override
    public void validateClientAndRedirectUri(String clientId, String redirectUri) {
        if (StringUtils.isEmpty(clientId)) {
            LOG.warn("client_id is missing");
            throw new AccessKeeperException(ErrorCodes.COMMON_MISSING_CLIENT_ID);
        }
        if (StringUtils.isEmpty(redirectUri)) {
            LOG.warn("redirect_uri is missing");
            throw new AccessKeeperException(ErrorCodes.COMMON_MISSING_REDIRECT);
        }

        if (redirectUri.length() > MAX_LENGTH_REDIRECT_URI) {
            LOG.warn("redirect_uri is too long");
            throw new AccessKeeperException(ErrorCodes.COMMON_INVALID_REDIRECT_URI);
        }

        final Client client = configService.getClientById(clientId);
        if (client == null) {
            LOG.warn("client_id is unknown");
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_CLIENT);
        }

        boolean validRedirect = false;
        for (String configuredUri : client.getValidRedirectUris()) {
            // @AFO: A_20434 - exact match oder Loopback URI mit ggf. unterschiedlichem Port
            if (StringUtils.equals(configuredUri, redirectUri) || LoopbackUtils.matchesLoopback(configuredUri, redirectUri)) {
                validRedirect = true;
                break;
            }
        }

        if (!validRedirect) {
            LOG.warn("redirect_uri doesn't match configured values");
            throw new AccessKeeperException(ErrorCodes.COMMON_INVALID_REDIRECT_URI);
        }
    }

    @Override
    public ChallengeDTO createChallenge(String responseType, String clientId, String state, String redirectUri, String scope,
                                        String codeChallenge, String codeChallengeMethod, String nonce) {

        validateAuthParameters(responseType, state, scope, codeChallenge, codeChallengeMethod, nonce);
        final List<String> scopes = ClaimUtils.getScopes(scope);
        validateFachdienst(scopes); // validate scope -> fd mapping

        Instant now = clock.instant();
        long epochSecond = now.getEpochSecond();

        JwtClaims challengeClaims = new JwtClaims();
        challengeClaims.setIssuer(configService.getIssuer(RequestContext.getRequestSource()));
        challengeClaims.setIssuedAt(epochSecond);
        challengeClaims.setExpiryTime(calculateChallengeExpiryTime(epochSecond)); // @AFO: A_20314-01 - G&uuml;ltigkeitsdauer des Challenge-Tokens wird gesetzt
        challengeClaims.setClaim(ClaimUtils.TOKEN_TYPE, TokenType.CHALLENGE.getId());
        challengeClaims.setClaim(ClaimUtils.TOKEN_ID, RandomUtils.randomUUID());

        // @AFO: A_20522 - neue Session-Id wird generiert und in die Challenge geschrieben
        final String sessionId = RandomUtils.randomShortUUID();
        challengeClaims.setClaim(ClaimUtils.SESSION_ID, sessionId);
        LogTool.setSessionId(sessionId);

        // @AFO: A_20698 - scope und codeChallenge werden in den Challenge-Token &uuml;bernommen
        challengeClaims.setClaim(ClaimUtils.SCOPE, scope);
        challengeClaims.setClaim(ClaimUtils.CODE_CHALLENGE, codeChallenge);
        challengeClaims.setClaim(ClaimUtils.CODE_CHALLENGE_METHOD, codeChallengeMethod);
        challengeClaims.setClaim(ClaimUtils.RESPONSE_TYPE, responseType);
        challengeClaims.setClaim(ClaimUtils.REDIRECT_URI, redirectUri);
        challengeClaims.setClaim(ClaimUtils.CLIENT_ID, clientId);
        challengeClaims.setClaim(ClaimUtils.STATE, state); // @AFO: A_20377 - State wird in den Challengetoken &uuml;bernommen

        if (nonce != null) {
            challengeClaims.setClaim(ClaimUtils.NONCE, nonce);
        }

        String challenge = challengeStrategy.toToken(challengeClaims);
        UserConsentDTO userConsent = createUserConsent(scopes);
        return new ChallengeDTO(challenge, userConsent); // @AFO: A_20523 - Der generierte UserConsent wird gemeinsam mit der Challenge retourniert
    }

    private ExtSessionEntity saveSessionEntity(String idpIss, String clientId, String state, String redirectUri, String scope, String codeChallenge,
                                               String nonce, String sessionId) {
        ExtSessionEntity sessionEntity = new ExtSessionEntity();
        sessionEntity.setState(sessionId);
        sessionEntity.setIdpIss(idpIss);

        sessionEntity.setClientId(clientId);
        sessionEntity.setClientRedirectUri(redirectUri);
        sessionEntity.setClientState(state);
        // @AFO: A_20698 - scope und codeChallenge werden in die Session &uuml;bernommen
        sessionEntity.setClientScope(scope);
        sessionEntity.setClientCodeChallenge(codeChallenge);
        sessionEntity.setClientNonce(nonce);

        // @AFO: A_22264 -  nonce und code_verifier werden generiert
        String idpCodeVerifier = PkceUtils.createCodeVerifier();
        String idpNonce = RandomUtils.randomShortUUID();
        sessionEntity.setIdpCodeVerifier(idpCodeVerifier);
        sessionEntity.setIdpNonce(idpNonce);

        sessionStorage.writeSession(sessionEntity);
        return sessionEntity;
    }

    private JwtClaims sessionToClaims(ExtSessionEntity session) {
        JwtClaims sessionClaims = new JwtClaims();
        sessionClaims.setClaim(ClaimUtils.SESSION_ID, session.getState());
        sessionClaims.setClaim(ClaimUtils.CLIENT_ID, session.getClientId());
        sessionClaims.setClaim(ClaimUtils.REDIRECT_URI, session.getClientRedirectUri());
        sessionClaims.setClaim(ClaimUtils.SCOPE, session.getClientScope());
        sessionClaims.setClaim(ClaimUtils.CODE_CHALLENGE_METHOD, OAuth2Constants.PKCE_METHOD_S256);
        sessionClaims.setClaim(ClaimUtils.CODE_CHALLENGE, session.getClientCodeChallenge());
        sessionClaims.setClaim(ClaimUtils.RESPONSE_TYPE, OAuth2Constants.RESPONSE_TYPE_CODE);
        sessionClaims.setClaim(ClaimUtils.STATE, session.getClientState());
        sessionClaims.setClaim(ClaimUtils.NONCE, session.getClientNonce());
        return sessionClaims;
    }

    /**
     * Creates the user consent based on the requested scopes. The consent contains:
     * <ul>
     *     <li>all requested scopes and their description
     *     <li>all requested claims and their description
     * </ul>
     *
     * @param scopes the list of requested scopes
     * @return the created user consent
     */
    // @AFO: A_20523 - Angeforderte Scopes werden geladen und der UserConsent anhand der Scopes und deren Claims zusammengestellt
    private UserConsentDTO createUserConsent(List<String> scopes) {
        Map<String, String> scopeMap = new HashMap<>();
        Map<String, String> claimMap = new HashMap<>();

        for (String scopeId : scopes) {
            final Scope scope = configService.getScopeById(scopeId);
            if (scope == null) {
                LOG.warn("Unknown scope '{}' was requested", scopeId);
                throw new AccessKeeperException(ErrorCodes.COMMON_INVALID_SCOPE);
            }

            scopeMap.put(scopeId, scope.getDescription());

            if (scope.getClaims() != null) {
                claimMap.putAll(scope.getClaims());
            }
        }
        return new UserConsentDTO(scopeMap, claimMap);
    }

    private void validateAuthParameters(String responseType, String state, String scope, String codeChallenge, String codeChallengeMethod, String nonce) {
        if (StringUtils.isEmpty(responseType)) {
            LOG.warn("response_type is missing");
            throw new AccessKeeperException(ErrorCodes.AUTH_MISSING_RESPONSE_TYPE);
        }
        else if (!OAuth2Constants.RESPONSE_TYPE_CODE.equals(responseType)) {
            LOG.warn("response_type is invalid");
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_RESPONSE_TYPE);
        }

        if (StringUtils.isEmpty(state)) {
            LOG.warn("state is missing");
            throw new AccessKeeperException(ErrorCodes.AUTH_MISSING_STATE_PARAMETER);
        }
        else if (!isValidVsChars(state, MAX_LENGTH_STATE)) {
            LOG.warn("state is invalid");
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_STATE_PARAMETER);
        }

        if (nonce != null && !isValidVsChars(nonce, MAX_LENGTH_NONCE)) {
            LOG.warn("nonce is invalid");
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_NONCE_PARAMETER);
        }

        if (StringUtils.isEmpty(scope)) {
            LOG.warn("scope is missing");
            throw new AccessKeeperException(ErrorCodes.COMMON_MISSING_SCOPE);
        }

        // @AFO: A_20434 - PKCE muss verwendet werden
        if (StringUtils.isEmpty(codeChallenge)) {
            LOG.warn("code_challenge is missing");
            throw new AccessKeeperException(ErrorCodes.AUTH_MISSING_CODE_CHALLENGE);
        }
        if (!isValidBase64UrlChars(codeChallenge, MAX_LENGTH_CODE_CHALLENGE)) {
            LOG.warn("code_challenge is invalid");
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_CODE_CHALLENGE);
        }

        if (!OAuth2Constants.PKCE_METHOD_S256.equals(codeChallengeMethod)) {
            LOG.warn("code_challenge_method is invalid");
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_CHALLENGE_METHOD);
        }
    }

    private void validateIdpIssParameter(String idpIss) {
        if (StringUtils.isBlank(idpIss)) {
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_MISSING_IDP_ISS);
        }
        if (idpIss.length() > MAX_LENGTH_IDP_ISS) {
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_IDP_ISS);
        }
        try {
            new URI(idpIss);
        }
        catch (URISyntaxException e) {
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_IDP_ISS, e);
        }
    }

    private void validateFederationAuthCodeParameters(String authorizationCode, String state) {
        if (StringUtils.isEmpty(authorizationCode)) {
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_MISSING_CODE);
        }
        if (StringUtils.isEmpty(state)) {
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_MISSING_STATE);
        }
        if (authorizationCode.length() > MAX_LENGTH_AUTH_CODE_SEK) {
            LOG.warn("fed auth_code is invalid");
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_CODE);
        }
        if (state.length() != 32 || !state.matches("\\p{XDigit}+")) {
            LOG.warn("state is invalid");
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_STATE);
        }
    }

    private void validateFachdienst(List<String> scopes) {
        Fachdienst fd = configService.getFachdienstByScope(getFdScope(scopes));

        if (fd == null) {
            LOG.warn("scope is not supported");
            throw new AccessKeeperException(ErrorCodes.COMMON_UNKNOWN_FD);
        }
    }

    private String getFdScope(JwtClaims claims) {
        return getFdScope(ClaimUtils.getScopes(claims));
    }

    private String getFdScope(List<String> scopes) {
        final List<String> scopesCopy = new ArrayList<>(scopes);

        boolean hasScopeOID = scopesCopy.remove(OAuth2Constants.SCOPE_OPENID);

        if (!hasScopeOID || scopesCopy.size() != 1) {
            LOG.warn("fd scope is missing");
            throw new AccessKeeperException(ErrorCodes.COMMON_INVALID_SCOPE);
        }

        return scopesCopy.get(0);
    }

    private boolean isValidVsChars(String input, int maxLength) {
        if (input.length() > maxLength) {
            return false;
        }
        return input.matches("[\\x20-\\x7E]+");
    }

    private boolean isValidBase64UrlChars(String input, int maxLength) {
        if (input.length() > maxLength) {
            return false;
        }
        return input.matches("[a-zA-Z0-9_-]+");
    }

    @Override
    public RedeemedChallengeDTO processSignedChallenge(String signedChallenge) {
        long authTime = clock.instant().getEpochSecond(); // @AFO: A_20731 - auth_time wird auf den Zeitpunkt des Einlangens der signierten Challenge gesetzt

        JwtClaims challengeClaims = signedChallengeExtractionStrategy.extractAndValidate(signedChallenge);
        LogTool.setSessionId(challengeClaims.getStringProperty(ClaimUtils.SESSION_ID));

        // @AFO: A_20951-01 neben der Signatur auch das Authentifizierungszertifikat anhand von OCSP überprüfen
        String autCertificate = challengeClaims.getStringProperty(ClaimUtils.CERTIFICATE);

        try {
            certificateServiceClient.validateClientCertificateAgainstOCSP(clock.instant(), autCertificate);
            return redeemChallenge(challengeClaims, autCertificate, authTime, AMR_METHODS);
        }
        catch (CertReaderException | CertificateServiceException e) {
            LOG.warn("auth certificate is invalid");
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_X509_CERT, e);
        }
    }

    @Override
    public RedeemedChallengeDTO processEncryptedSignedAuthenticationData(String encryptedSignedAuthenticationData) {
        Instant instantNow = clock.instant();
        long authTime = instantNow.getEpochSecond();  // @AFO: A_20731 - auth_time wird auf den Zeitpunkt des Einlangens der signierten Challenge gesetzt

        IdpJwsJwtCompactConsumer signedAuthenticationData = authenticationDataExtractionStrategy.extractAndValidate(encryptedSignedAuthenticationData);
        String encodedSignedAuthenticationData = signedAuthenticationData.getEncodedJws();

        JwtClaims signedAuthenticationDataJwtClaims = signedAuthenticationData.getJwtClaims();

        String challengeTokenClaim = signedAuthenticationDataJwtClaims.getStringProperty(ClaimUtils.CHALLENGE_TOKEN);
        JwtClaims challengeClaims;
        try {
            challengeClaims = challengeExtractionStrategy.extractAndValidate(challengeTokenClaim);
        }
        catch (AccessKeeperException a) {
            LOG.warn("challenge is not valid");
            throw new AccessKeeperException(ErrorCodes.VAL1_ALT_AUTH_FAILED, a);
        }
        LogTool.setSessionId(challengeClaims.getStringProperty(ClaimUtils.SESSION_ID));

        List<String> amr = extractAmr(signedAuthenticationDataJwtClaims);

        String autCertificateBase64UrlEncoding = signedAuthenticationDataJwtClaims.getStringProperty(ClaimUtils.AUTH_CERT);
        byte[] autCertBytes;
        try {
            autCertBytes = Base64.getUrlDecoder().decode(autCertificateBase64UrlEncoding);
        }
        catch (Exception e) {
            LOG.warn("auth certificate is not valid base64 encoded");
            throw new AccessKeeperException(ErrorCodes.VAL1_ALT_AUTH_FAILED);
        }
        // certs in x5c claims are base64 encoded. alt-auth certs are base64url encoded. therefore we need to transcode it
        String autCertBase64 = Base64.getEncoder().encodeToString(autCertBytes);

        // run steps in parallel and wait until all are finished or at least one fails
        Pair<Void, AuthenticationResponse> allResults = runAlternativeAuthSteps(
            // validate authentication certificate
            // @AFO: A_21433 - das Zertifikat wird anhand der Systemzeit &uuml;berpr&uuml;ft
            runAsync(() -> certificateServiceClient.validateClientCertificateAgainstOCSP(instantNow, autCertBytes), taskExecutor),

            // authenticate client device using Pairing Dienst
            supplyAsync(() -> pairingDienstClient.authenticateClientDevice(new SignedAuthenticationDataRequest(encodedSignedAuthenticationData)), taskExecutor)
        );

        AuthenticationResponse authenticationResponse = allResults.getRight();

        // compare challenge token extracted by Pairing-Dienst with challenge token extracted by this method
        if (!StringUtils.equals(authenticationResponse.getChallengeToken(), challengeTokenClaim)) {
            LOG.error("received challenge token does not match extracted challenge token");
            throw new AccessKeeperException(ErrorCodes.VAL1_ALT_AUTH_FAILED);
        }

        try {
            // @AFO: 21440 - AUTH_CODE und SSO_TOKEN werden anhand des &uuml;bermittelten Auth-Zertifikats und des extrahierten amr-Claims bef&uuml;llt
            return redeemChallenge(challengeClaims, autCertBase64, authTime, amr);
        }
        catch (CertReaderException e) {
            LOG.warn("auth certificate is invalid");
            throw new AccessKeeperException(ErrorCodes.VAL1_ALT_AUTH_FAILED, e);
        }
    }

    // @AFO: A_21440 - amr-Claim wird aus den Authentisierungsdaten extrahiert
    private List<String> extractAmr(JwtClaims claims) {
        try {
            List<String> amr = claims.getListStringProperty(ClaimUtils.AUTH_METHOD);
            if (amr.size() > MAX_AMR_VALUES) {
                LOG.warn("amr value contains too many entries");
                throw new IllegalArgumentException("amr value contains too many entries");
            }
            if (amr.stream().anyMatch(e -> e.length() > MAX_LENGTH_AMR)) {
                LOG.warn("amr entry is too long");
                throw new IllegalArgumentException("amr entry is too long");
            }
            if (amr.contains(OAuth2Constants.AMR_SMART_CARD)) {
                LOG.warn("amr value 'smart card' is not allowed when using alternative authentication");
                throw new IllegalArgumentException("amr value 'smart card' is not allowed when using alternative authentication");
            }
            return amr;
        }
        catch (Exception e) {
            LOG.warn("amr claim is not valid");
            throw new AccessKeeperException(ErrorCodes.VAL1_ALT_AUTH_FAILED);
        }
    }

    // @AFO: 21440 - AUTH_CODE und SSO_TOKEN werden mit den Zertifikatsclaims und dem amr-Wert bef&uuml;llt
    private RedeemedChallengeDTO redeemChallenge(JwtClaims challengeClaims, String autCertificate, long authTime, List<String> amrMethods) throws CertReaderException {
        // @AFO: A_20440-01 redirect_uri aus CHALLENGE_TOKEN wird gegen registrierte URIs des Clients validiert
        String clientId = challengeClaims.getStringProperty(ClaimUtils.CLIENT_ID);
        validateClientAndRedirectUri(clientId, challengeClaims.getStringProperty(ClaimUtils.REDIRECT_URI));

        X509Certificate x509Certificate = certReader.parseCertificate(autCertificate);
        Map<String, String> cardClaims = certReader.extractCertificateClaims(x509Certificate);

        String fdScope = getFdScope(challengeClaims);
        JwtClaims authCodeClaims = createAuthCode(authTime, authTime, fdScope, challengeClaims, cardClaims, amrMethods);
        String authCode = authCodeStrategy.toToken(authCodeClaims);

        Client client = configService.getClientById(clientId);
        String ssoToken = null;
        if (client.isNeedsSsoToken()) {
            JwtClaims ssoClaims = createSsoTokenFromCertificate(authTime, authCodeClaims, autCertificate);
            // @AFO: A_20696 - Verschlüsselung des "SSO_TOKEN"
            ssoToken = ssoStrategy.toToken(ssoClaims);
        }

        String redirectUri = challengeClaims.getStringProperty(ClaimUtils.REDIRECT_URI);
        String state = challengeClaims.getStringProperty(ClaimUtils.STATE);

        return new RedeemedChallengeDTO(ssoToken, authCode, redirectUri, state);
    }

    private JwtClaims createAuthCode(long authTime, long now, String fdScope, JwtClaims challengeClaims, Map<String, String> cardClaims, List<String> amrMethods) {
        Scope scope = configService.getScopeById(fdScope);
        if (scope == null) {
            LOG.warn("unknown scope");
            throw new AccessKeeperException(ErrorCodes.COMMON_INVALID_SCOPE);
        }

        JwtClaims authCodeClaims = new JwtClaims();
        authCodeClaims.setIssuer(configService.getIssuer(RequestContext.getRequestSource()));
        authCodeClaims.setIssuedAt(now);
        authCodeClaims.setExpiryTime(calculateAuthCodeExpiryTime(now));  // @AFO: A_20314-01 - G&uuml;ltigkeitsdauer des AUTH_CODEs wird gesetzt

        authCodeClaims.setClaim(ClaimUtils.TOKEN_ID, RandomUtils.randomUUID());
        authCodeClaims.setClaim(ClaimUtils.AUTH_TIME, authTime);
        authCodeClaims.setClaim(ClaimUtils.TOKEN_TYPE, TokenType.AUTH_CODE.getId());

        // @AFO: A_20697 - Die relevanten Claims aus der Challenge werden in den AUTH_CODE &uuml;bernommen
        ClaimUtils.copy(challengeClaims, authCodeClaims, ClaimUtils.SESSION_ID, ClaimUtils.CLIENT_ID, ClaimUtils.REDIRECT_URI,
            ClaimUtils.SCOPE, ClaimUtils.CODE_CHALLENGE_METHOD, ClaimUtils.CODE_CHALLENGE, ClaimUtils.RESPONSE_TYPE, ClaimUtils.STATE, ClaimUtils.NONCE);

        // copy requested card claims from AUT cert
        // @AFO: A_20949 - &Uuml;berpr&uuml;fung ob alle erforderlichen Claims im SSO Token vorhanden sind
        // @AFO: A_20313-01 - Die mit dem FD in der Registrierung abgestimmten Claims seines Scopes werden in den AuthCode &uuml;bernommen
        for (String claimName : scope.getClaims().keySet()) {
            boolean containsClaim = cardClaims.containsKey(claimName);
            if (!containsClaim) {
                LOG.warn("claim {} was not consented", claimName);
                throw new AccessKeeperException(ErrorCodes.AUTH_REQUESTED_CLAIMS_NOT_CONSENTED);
            }
            else {
                authCodeClaims.setClaim(claimName, cardClaims.get(claimName));
            }
        }

        authCodeClaims.setProperty(ClaimUtils.AUTH_METHOD, amrMethods);

        return authCodeClaims;
    }

    private JwtClaims createSsoTokenFromCertificate(long now, JwtClaims authCodeClaims, String autCertificate) {
        JwtClaims ssoClaims = createSsoToken(now, authCodeClaims);
        ssoClaims.setClaim(ClaimUtils.CERTIFICATE, autCertificate);
        ssoClaims.setClaim(ClaimUtils.SSO_GRANT_TYPE, ClaimUtils.SSO_GRANT_TYPE_CERT);
        return ssoClaims;
    }

    private JwtClaims createSsoTokenFromFedIdToken(long now, JwtClaims authCodeClaims) {
        JwtClaims ssoClaims = createSsoToken(now, authCodeClaims);
        ssoClaims.setClaim(ClaimUtils.SSO_GRANT_TYPE, ClaimUtils.SSO_GRANT_TYPE_FEDTOKEN);
        return ssoClaims;
    }

    // @AFO: A_20694 - Setzt alle Attribute des SSO-Tokens
    private JwtClaims createSsoToken(long now, JwtClaims authCodeClaims) {
        JwtClaims ssoClaims = new JwtClaims();
        ssoClaims.setIssuer(configService.getIssuer(RequestContext.getRequestSource()));
        ssoClaims.setIssuedAt(now);
        // @AFO: A_20692-01 - Die maximale Gültigkeit wird als Platzhalter in den Token geschrieben
        ssoClaims.setExpiryTime(now + MAX_SSO_EXPIRATION);

        ssoClaims.setClaim(ClaimUtils.TOKEN_ID, RandomUtils.randomUUID());
        ssoClaims.setClaim(ClaimUtils.TOKEN_TYPE, TokenType.SSO.getId());

        ClaimUtils.copy(authCodeClaims, ssoClaims, ClaimUtils.AUTH_TIME, ClaimUtils.CLIENT_ID, ClaimUtils.REDIRECT_URI, ClaimUtils.AUTH_METHOD);
        ClaimUtils.copy(authCodeClaims, ssoClaims, ClaimUtils.CARD_CLAIMS); // copy all consented card claims

        return ssoClaims;
    }

    @Override
    public RedeemedSsoTokenDTO redeemSsoToken(String ssoToken, String unsignedChallenge) {
        Instant now = clock.instant();
        long epochSecond = now.getEpochSecond();

        // parse incoming tokens
        JwtClaims challengeClaims = challengeExtractionStrategy.extractAndValidate(unsignedChallenge);
        LogTool.setSessionId(challengeClaims.getStringProperty(ClaimUtils.SESSION_ID)); // write the session id to the MDC

        // @AFO: A_20440-01 redirect_uri aus CHALLENGE_TOKEN wird gegen registrierte URIs des Clients validiert
        validateClientAndRedirectUri(challengeClaims.getStringProperty(ClaimUtils.CLIENT_ID), challengeClaims.getStringProperty(ClaimUtils.REDIRECT_URI));

        // @AFO: A_20946-01 Ueberpruefung ob dieser Client einen SSO_TOKEN einloesen darf
        Client client = configService.getClientById(challengeClaims.getStringProperty(ClaimUtils.CLIENT_ID));
        if (client == null || !client.isNeedsSsoToken()) {
            throw new AccessKeeperException(ErrorCodes.AUTH_SSO_TOKEN_NOT_CONFIGURED);
        }

        String fdScope = getFdScope(challengeClaims); // @AFO: A_20950-1 - Scope des Fachdiensts wird aus der Challenge extrahiert

        // @AFO: A_20947 - Entschlüsselung des "SSO_TOKEN"
        JwtClaims ssoTokenClaims = ssoTokenExtractionStrategy.extractAndValidate(ssoToken);

        validateSsoTokenAndChallenge(ssoTokenClaims, challengeClaims, epochSecond, fdScope);

        final String grantType = ssoTokenClaims.getStringProperty(ClaimUtils.SSO_GRANT_TYPE);
        if (ClaimUtils.SSO_GRANT_TYPE_CERT.equals(grantType)) {
            String autCertificate = ssoTokenClaims.getStringProperty(ClaimUtils.CERTIFICATE);
            if (autCertificate == null) {
                throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_SSO_TOKEN);
            }
            try {
                // @AFO: A_20951-01 neben der Signatur auch das Authentifizierungszertifikat anhand von OCSP überprüfen
                certificateServiceClient.validateClientCertificateAgainstOCSP(clock.instant(), autCertificate);
            }
            catch (CertificateServiceException e) {
                // @AFO: A_20949 neue Authentisierung anfordern, wenn die Validierung fehlschlägt
                throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_SSO_TOKEN, e);
            }
        }

        // @AFO: A_20459 Übernahme des Attribute AUTH_TIME aus den SSO Token
        Long authTime = ssoTokenClaims.getLongProperty(ClaimUtils.AUTH_TIME);

        // extract auth methods from sso token
        List<String> authMethods = ssoTokenClaims.getListStringProperty(ClaimUtils.AUTH_METHOD);

        // @AFO: A_20950-01 - AUTH_CODE wird nach erfolgreicher Verarbeitung des SSO Tokens f&uuml;r den Scope ausgestellt
        JwtClaims authCodeClaims = createAuthCode(authTime, epochSecond, fdScope, challengeClaims, extractCardClaims(ssoTokenClaims), authMethods);
        String authCode = authCodeStrategy.toToken(authCodeClaims);

        return new RedeemedSsoTokenDTO(authCode, challengeClaims.getStringProperty(ClaimUtils.REDIRECT_URI), challengeClaims.getStringProperty(ClaimUtils.STATE));
    }

    @Override
    public String startFederatedAuthorization(String idpIss, String responseType, String clientId, String appState, String appRedirectUri, String scope,
                                              String appCodeChallenge, String appCodeChallengeMethod, String appNonce) {
        validateAuthParameters(responseType, appState, scope, appCodeChallenge, appCodeChallengeMethod, appNonce);
        validateIdpIssParameter(idpIss);
        LogTool.setIdpIss(idpIss);

        final List<String> scopes = ClaimUtils.getScopes(scope);
        validateFachdienst(scopes); // validate scope -> fd mapping

        // @AFO: A_23687 - Überprüfung ob es sich um einen bekannten sektoralen IDP handelt
        EntityStatementDTO entityStatementDTO = entityStatementSynchronizationService.getEntityStatementCache(idpIss);

        // @AFO: A_20522 - neue Session-Id wird generiert und in die Session geschrieben
        String sessionId = sessionStorage.createSessionId();
        LogTool.setSessionId(sessionId);
        LOG.info("Created session");

        ExtSessionEntity sessionEntity = saveSessionEntity(idpIss, clientId, appState, appRedirectUri, scope, appCodeChallenge, appNonce, sessionId);
        String requestUri = extParService.sendParRequest(entityStatementDTO, sessionEntity, appRedirectUri);

        String authEndpointUrl = entityStatementDTO.getAuthorizationEndpoint();
        String issuer = configService.getIssuer(RequestSource.INTERNET);

        // @AFO: A_23688 - Authorizationrequest an Auth-Endpunkt des sektoralen IDPs wird erstellt
        UrlBuilder urlBuilder = new UrlBuilder(authEndpointUrl)
            .appendUriParameter(ClaimUtils.CLIENT_ID, issuer)
            .appendUriParameter(ClaimUtils.REQUEST_URI, requestUri);

        return urlBuilder.toString();
    }

    // @AFO: A_20949 - &Uuml;berpr&uuml;fung ob SSO Token und Challenge konsistent sind
    private void validateSsoTokenAndChallenge(JwtClaims ssoTokenClaims, JwtClaims challengeClaims, long epochSecond, String fdScope) {

        // @AFO: A_20948-01 - AUTH_TIME wird aus dem SSO Token ausgelesen. Aus AUTH_TIME + konfigurierter G&uuml;ltigkeit des SSO Tokens f&uuml;r den FD wird die Expiry berechnet.
        // Dieser Wert darf nicht kleiner sein als die aktuelle Zeit
        Long authTime = ssoTokenClaims.getLongProperty(ClaimUtils.AUTH_TIME);
        long ssoExpiryTime = calculateSSOExpiryTime(fdScope, authTime);
        if (epochSecond > ssoExpiryTime) {
            LOG.warn("SSO token is expired [fdScope={}], [authTime={}], [expiry={}]", fdScope, authTime, ssoExpiryTime);
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_SSO_TOKEN);
        }

        String ssoGrantType = ssoTokenClaims.getStringProperty(ClaimUtils.SSO_GRANT_TYPE);
        if (!ClaimUtils.SSO_GRANT_TYPES.contains(ssoGrantType)) {
            LOG.warn("Invalid grant type: {}", ssoGrantType);
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_SSO_TOKEN);
        }

        String ssoClientId = ssoTokenClaims.getStringProperty(ClaimUtils.CLIENT_ID);
        String challengeClientId = challengeClaims.getStringProperty(ClaimUtils.CLIENT_ID);
        if (!StringUtils.equals(ssoClientId, challengeClientId)) {
            LOG.warn("SSO token client_id '{}' and challenge client_id '{}' don't match", ssoClientId, challengeClientId);
            throw new AccessKeeperException(ErrorCodes.AUTH_SSO_TOKEN_CHALLENGE_MISMATCH);
        }

        String ssoRedirectUri = ssoTokenClaims.getStringProperty(ClaimUtils.REDIRECT_URI);
        String challengeRedirectUri = challengeClaims.getStringProperty(ClaimUtils.REDIRECT_URI);
        if (!StringUtils.equals(ssoRedirectUri, challengeRedirectUri)) {
            LOG.warn("SSO token redirect_uri '{}' and challenge redirect_uri '{}' don't match", ssoRedirectUri, challengeRedirectUri);
            throw new AccessKeeperException(ErrorCodes.AUTH_SSO_TOKEN_CHALLENGE_MISMATCH);
        }
    }

    @Override
    public RedeemedChallengeDTO redeemFedAuthCode(String authorizationCode, String state) {
        long authTime = clock.instant().getEpochSecond();
        validateFederationAuthCodeParameters(authorizationCode, state);

        ExtSessionEntity session = sessionStorage.getSession(state);
        if (session == null) {
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_UNKNOWN_SESSION);
        }
        LogTool.setIdpIss(session.getIdpIss());
        try {
            EntityStatementDTO entityStatement = entityStatementSynchronizationService.getEntityStatementCache(session.getIdpIss());

            // @AFO: A_23049 - Abruf des ID-Tokens
            String idToken = extAuthCodeService.redeemFedAuthCode(entityStatement, authorizationCode, session.getIdpCodeVerifier(), session.getClientRedirectUri());
            LOG.info("Received id_token from federation idp [state={}]", state);

            Map<String, Object> idTokenValidationContext = new HashMap<>();
            idTokenValidationContext.put(FederationIdTokenExtractionStrategy.CONTEXT_ENTITY_STATEMENT, entityStatement);
            idTokenValidationContext.put(FederationIdTokenExtractionStrategy.CONTEXT_SESSION, session);

            final JwtClaims idTokenClaims = federationIdTokenExtractionStrategy.extractAndValidate(idToken, idTokenValidationContext);
            Map<String, String> federationClaims = extractFederationClaims(idTokenClaims);
            JwtClaims sessionClaims = sessionToClaims(session);
            JwtClaims authCodeClaims = createAuthCode(authTime, authTime, getFdScope(sessionClaims), sessionClaims, federationClaims,
                Collections.singletonList(OAuth2Constants.AMR_MULTI_FACTOR_AUTH));

            Client client = configService.getClientById(session.getClientId());
            String ssoToken = null;
            if (client.isNeedsSsoToken()) {
                JwtClaims ssoClaims = createSsoTokenFromFedIdToken(authTime, authCodeClaims);
                // @AFO: A_20696 - Verschlüsselung des "SSO_TOKEN"
                ssoToken = ssoStrategy.toToken(ssoClaims);
            }

            return new RedeemedChallengeDTO(ssoToken, authCodeStrategy.toToken(authCodeClaims), session.getClientRedirectUri(), session.getClientState());
        }
        finally {
            sessionStorage.destroySession(state);
        }
    }

    private Map<String, String> extractCardClaims(JwtClaims ssoTokenClaims) {
        Map<String, String> cardClaims = new HashMap<>();
        for (String claimName : ClaimUtils.CARD_CLAIMS) {
            if (ssoTokenClaims.containsProperty(claimName)) {
                cardClaims.put(claimName, ssoTokenClaims.getStringProperty(claimName));
            }
        }
        return cardClaims;
    }

    private Map<String, String> extractFederationClaims(JwtClaims idTokenClaims) {
        Map<String, String> cardClaims = new HashMap<>();

        if (idTokenClaims.containsProperty(ClaimUtils.URN_PROFESSION)) {
            cardClaims.put(ClaimUtils.PROFESSION, idTokenClaims.getStringProperty(ClaimUtils.URN_PROFESSION));
        }
        if (idTokenClaims.containsProperty(ClaimUtils.URN_ORGANIZATION)) {
            cardClaims.put(ClaimUtils.ORG_NAME, idTokenClaims.getStringProperty(ClaimUtils.URN_ORGANIZATION));
            cardClaims.put(ClaimUtils.ORG_IK_NUMBER, idTokenClaims.getStringProperty(ClaimUtils.URN_ORGANIZATION));
        }
        if (idTokenClaims.containsProperty(ClaimUtils.URN_ID)) {
            cardClaims.put(ClaimUtils.ID_NUMBER, idTokenClaims.getStringProperty(ClaimUtils.URN_ID));
        }
        if (idTokenClaims.containsProperty(ClaimUtils.URN_DISPLAY_NAME)) {
            cardClaims.put(ClaimUtils.DISPLAY_NAME, idTokenClaims.getStringProperty(ClaimUtils.URN_DISPLAY_NAME));
        }
        cardClaims.put(ClaimUtils.FAMILY_NAME, "");
        cardClaims.put(ClaimUtils.GIVEN_NAME, "");
        return cardClaims;
    }

    /**
     * Calculates expiry time depending on the configuration. The expiry time must not exceed 24 hours.
     *
     * @param scope    the requested scope
     * @param authTime timestamp of the last authentication using an approved mechanism (e.g. smartcard)
     * @return expiry time calculated using configuration and iat
     * @AFO A_20313-01 - Berechnet die G&uuml;ltigkeitsdauer des SSO-Tokens
     * @AFO A_20692-01 - Begrenzt die G&uuml;ltigkeitsdauer des SSO-Tokens auf 24h
     */
    private long calculateSSOExpiryTime(String scope, long authTime) {
        Fachdienst fachdienst = configService.getFachdienstByScope(scope);
        if (fachdienst == null) {
            LOG.warn("unknown scope");
            throw new AccessKeeperException(ErrorCodes.COMMON_INVALID_SCOPE);
        }
        Long timeout = fachdienst.getSsoTokenExpires();
        if (timeout == null || timeout < 0 || timeout > MAX_SSO_EXPIRATION) {
            return authTime + MAX_SSO_EXPIRATION;
        }
        return authTime + timeout;
    }

    /**
     * Calculates expiry time depending on the configuration. The expiry time must not exceed 60 seconds.
     *
     * @param iat time token has been issued
     * @return expiry time calculated using configuration and iat
     */
    // @AFO: A_20314-01 - Begrenzt die Gültigkeitsdauer des AUTH_CODEs auf 60 Sekunden
    private long calculateAuthCodeExpiryTime(long iat) {
        Long timeout = configService.getTokenTimeout(TokenType.AUTH_CODE);
        if (timeout == null || timeout < 0 || timeout > MAX_AUTH_CODE_EXPIRATION) {
            return iat + MAX_AUTH_CODE_EXPIRATION;
        }
        return iat + timeout;
    }

    /**
     * Calculates expiry time depending on the configuration. The expiry time must not exceed 180 seconds.
     *
     * @param iat time challenge has been issued
     * @return expiry time calculated using configuration and iat
     * @AFO A_20314-01 - Begrenzt die G&uuml;ltigkeitsdauer des Challenge-Tokens auf 180s
     */
    private long calculateChallengeExpiryTime(long iat) {
        Long timeout = configService.getTokenTimeout(TokenType.CHALLENGE);
        if (timeout == null || timeout < 0 || timeout > MAX_CHALLENGE_EXPIRATION) {
            return iat + MAX_CHALLENGE_EXPIRATION;
        }
        return iat + timeout;
    }

    private <T1, T2> Pair<T1, T2> runAlternativeAuthSteps(CompletableFuture<T1> step1, CompletableFuture<T2> step2) {
        try {
            CompletableFuture.allOf(step1, step2).get();
            return ImmutablePair.of(step1.get(), step2.get());
        }
        catch (InterruptedException i) {
            Thread.currentThread().interrupt();
            LOG.warn("threas was interrupted");
            throw new AccessKeeperException(ErrorCodes.VAL1_ALT_AUTH_FAILED, i);
        }
        catch (ExecutionException | CancellationException e) {
            if (e.getCause() instanceof AccessKeeperException) {
                throw (AccessKeeperException) e.getCause();
            }
            LOG.warn("one or both alternative authentication steps failed");

            // @AFO: A_21438 u. A_21435, A_21433 - falls der Aufruf des Pairing Fachdiensts einen Fehler liefert, antwortet der Access Keeper mit Fehler VAL.1
            throw new AccessKeeperException(ErrorCodes.VAL1_ALT_AUTH_FAILED, e);
        }
    }
}

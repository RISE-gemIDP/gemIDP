/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.api.authorization;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.util.UrlBuilder;
import com.rise_world.gematik.accesskeeper.server.dto.ChallengeDTO;
import com.rise_world.gematik.accesskeeper.server.dto.RedeemedChallengeDTO;
import com.rise_world.gematik.accesskeeper.server.dto.RedeemedSsoTokenDTO;
import com.rise_world.gematik.accesskeeper.server.exception.AuthorizationException;
import com.rise_world.gematik.accesskeeper.server.service.AuthorizationService;
import com.rise_world.gematik.idp.server.api.authorization.AuthorizationEndpoint;
import com.rise_world.gematik.idp.server.api.authorization.ChallengeResponse;
import com.rise_world.gematik.idp.server.api.authorization.UserConsent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.net.URI;

import static com.rise_world.gematik.accesskeeper.server.api.util.ParameterUtils.validateDuplicateClientIdRedirectUri;
import static com.rise_world.gematik.accesskeeper.server.api.util.ParameterUtils.validateDuplicateParameters;

// @AFO: A_20686 - separates Interface f&uuml;r AUTH
@RestController
public class AuthorizationEndpointImpl implements AuthorizationEndpoint {

    private static final String CODE = "code";
    private static final String SSO_TOKEN = "ssotoken";

    private AuthorizationService authorizationService;
    private HttpServletRequest servletRequest;

    @Autowired
    public AuthorizationEndpointImpl(AuthorizationService authorizationService, HttpServletRequest servletRequest) {
        this.authorizationService = authorizationService;
        this.servletRequest = servletRequest;
    }

    @Override
    // @AFO: A_20698 - scope und codeChallenge werden an der Schnittstelle angenommen
    public Response startAuthorization(String responseType,
                                       String clientId,
                                       String state,
                                       String redirectUri,
                                       String scope,
                                       String codeChallenge,
                                       String codeChallengeMethod,
                                       String nonce) {

        // @AFO: A_20434 - keine doppelten Parameter
        validateDuplicateClientIdRedirectUri(servletRequest);
        authorizationService.validateClientAndRedirectUri(clientId, redirectUri);

        ChallengeDTO challengeDTO;
        try {
            validateDuplicateParameters(servletRequest, ErrorCodes.AUTH_DUPLICATE_PARAMETERS);
            challengeDTO = authorizationService.createChallenge(responseType, clientId, state, redirectUri, scope, codeChallenge, codeChallengeMethod, nonce);
        }
        catch (AccessKeeperException e) {
            // clientId and redirectUri were successfully validated. send error as redirect
            // @AFO: A_20376 - "state"-Parameter wird im Fehlerfall - falls redirectet wird - im Redirect verwendet
            throw new AuthorizationException(e.getErrorMessage(), redirectUri, state, e);
        }
        catch (Exception e) {
            // clientId and redirectUri were successfully validated. send error as redirect
            // @AFO: A_20376 - "state"-Parameter wird im Fehlerfall - falls redirectet wird - im Redirect verwendet
            throw new AuthorizationException(ErrorCodes.AUTH_INTERNAL_SERVER_ERROR, redirectUri, state, e);
        }

        final ChallengeResponse challengeResponse = new ChallengeResponse();
        challengeResponse.setChallenge(challengeDTO.getChallenge());

        final UserConsent userConsent = new UserConsent();
        userConsent.setRequestedClaims(challengeDTO.getUserConsent().getRequestedClaims());
        userConsent.setRequestedScopes(challengeDTO.getUserConsent().getRequestedScopes());
        // @AFO: A_20521-02 - Challenge-Token wird gemeinsam mit dem UserConsent an den Authenticator geschickt
        challengeResponse.setUserConsent(userConsent);

        return Response.ok(challengeResponse).build();
    }

    @Override
    // @AFO: A_20699-02 - Signierter und verschl&uuml;sselter CHALLENGE_TOKEN wird angenommen
    public Response finishAuthorizationWithSignature(String signedChallenge) {
        // @AFO: A_20434 - keine doppelten Parameter
        validateDuplicateParameters(servletRequest, ErrorCodes.AUTH_DUPLICATE_PARAMETERS);

        RedeemedChallengeDTO redeemedChallenge = authorizationService.processSignedChallenge(signedChallenge);

        // AFO: A_20376 - "state"-Parameter wird in Redirect verwendet
        String redirectUri = new UrlBuilder(redeemedChallenge.getRedirectUri())
            .appendParameter(CODE, redeemedChallenge.getAuthCode())
            .appendParameter(SSO_TOKEN, redeemedChallenge.getSsoToken())
            .appendState(redeemedChallenge.getState())
            .toString();
        return Response.status(Response.Status.FOUND).location(URI.create(redirectUri)).build();
    }

    @Override
    // @AFO: A_20946-01 - SSO Token wird an der Schnittstelle angenommen
    public Response finishAuthorizationWithSsoToken(String ssoToken,
                                                    String unsignedChallenge) {
        // @AFO: A_20434 - keine doppelten Parameter
        validateDuplicateParameters(servletRequest, ErrorCodes.AUTH_DUPLICATE_PARAMETERS);

        RedeemedSsoTokenDTO redeemedSsoToken = authorizationService.redeemSsoToken(ssoToken, unsignedChallenge);

        // AFO: A_20376 - "state"-Parameter wird in Redirect verwendet
        String redirectUri = new UrlBuilder(redeemedSsoToken.getRedirectUri())
            .appendParameter(CODE, redeemedSsoToken.getAuthCode())
            .appendState(redeemedSsoToken.getState())
            .toString();

        return Response.status(Response.Status.FOUND).location(URI.create(redirectUri)).build();
    }

    @Override
    public Response finishAuthorizationWithAlternativeSignature(String encryptedSignedAuthenticationData) {
        // @AFO: A_20434 - keine doppelten Parameter
        validateDuplicateParameters(servletRequest, ErrorCodes.AUTH_DUPLICATE_PARAMETERS);

        RedeemedChallengeDTO redeemedChallenge = authorizationService.processEncryptedSignedAuthenticationData(encryptedSignedAuthenticationData);

        // AFO: A_20376 - "state"-Parameter wird in Redirect verwendet
        String redirectUri = new UrlBuilder(redeemedChallenge.getRedirectUri())
            .appendParameter(CODE, redeemedChallenge.getAuthCode())
            .appendParameter(SSO_TOKEN, redeemedChallenge.getSsoToken())
            .appendState(redeemedChallenge.getState())
            .toString();
        return Response.status(Response.Status.FOUND).location(URI.create(redirectUri)).build();
    }
}

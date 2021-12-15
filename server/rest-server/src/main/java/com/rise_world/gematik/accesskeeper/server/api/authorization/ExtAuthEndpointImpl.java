/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.api.authorization;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.util.UrlBuilder;
import com.rise_world.gematik.accesskeeper.server.dto.RedeemedChallengeDTO;
import com.rise_world.gematik.accesskeeper.server.exception.AuthorizationException;
import com.rise_world.gematik.accesskeeper.server.service.AuthorizationService;
import com.rise_world.gematik.idp.server.api.authorization.ExtAuthEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import java.net.URI;

import static com.rise_world.gematik.accesskeeper.server.api.util.ParameterUtils.validateDuplicateClientIdRedirectUri;
import static com.rise_world.gematik.accesskeeper.server.api.util.ParameterUtils.validateDuplicateParameters;

// @AFO: A_20686 - separates Interface f&uuml;r (EXT)AUTH
@RestController
public class ExtAuthEndpointImpl implements ExtAuthEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(ExtAuthEndpointImpl.class);

    private AuthorizationService authorizationService;
    private HttpServletRequest servletRequest;

    @Autowired
    public ExtAuthEndpointImpl(AuthorizationService authorizationService, HttpServletRequest servletRequest) {
        this.authorizationService = authorizationService;
        this.servletRequest = servletRequest;
    }

    @Override
    // @AFO: A_20698 - scope und codeChallenge werden an der Schnittstelle angenommen
    // @AFO: A_22264 - kk_app_id wird an der Schnittstelle angenommen
    public Response startExtAuthorization(String kkAppId, String responseType, String clientId, String state, String redirectUri, String scope,
                                          String codeChallenge, String codeChallengeMethod, String nonce) {

        // @AFO: A_20434 - keine doppelten Parameter
        validateDuplicateClientIdRedirectUri(servletRequest);
        authorizationService.validateClientAndRedirectUri(clientId, redirectUri);

        try {
            validateDuplicateParameters(servletRequest, ErrorCodes.AUTH_DUPLICATE_PARAMETERS);
            String sektorIdpAuthUrl = authorizationService.startExternalAuthorization(kkAppId, responseType, clientId, state, redirectUri, scope,
                codeChallenge, codeChallengeMethod, nonce);

            // @AFO: A_22264 - Authorization Request wird als HTTP redirect an den Aufrufer geschickt
            return Response.status(Response.Status.FOUND).location(URI.create(sektorIdpAuthUrl)).build();
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
    }

    @Override
    // @AFO: A_22265 - AUTHORIZATION_CODE_IDP, state und kk_app_redirect_uri werden an der Schnittstelle angenommen
    public Response finishAuthorizationWithExtCode(String authorizationCode, String state, String kkAppRedirectUri) {
        LOG.info("Received authcode for [state={}] and [kk_app_redirect_uri={}]", state, kkAppRedirectUri);

        validateDuplicateParameters(servletRequest, ErrorCodes.AUTH_DUPLICATE_PARAMETERS);

        RedeemedChallengeDTO redeemedExtCode = authorizationService.redeemExternalAuthCode(authorizationCode, state, kkAppRedirectUri);

        String redirectUri = new UrlBuilder(redeemedExtCode.getRedirectUri())
            .appendParameter("code", redeemedExtCode.getAuthCode())
            .appendParameter("ssotoken", redeemedExtCode.getSsoToken())
            .appendState(redeemedExtCode.getState())
            .toString();

        return Response.status(Response.Status.FOUND).location(URI.create(redirectUri)).build();
    }
}

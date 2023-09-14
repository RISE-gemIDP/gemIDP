/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.api.authserver;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.util.UrlBuilder;
import com.rise_world.gematik.accesskeeper.server.dto.RedeemedChallengeDTO;
import com.rise_world.gematik.accesskeeper.server.exception.AuthorizationException;
import com.rise_world.gematik.accesskeeper.server.service.AuthorizationService;
import com.rise_world.gematik.idp.server.api.authorization.FedAuthEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.net.URI;

import static com.rise_world.gematik.accesskeeper.server.api.util.ParameterUtils.validateDuplicateClientIdRedirectUri;
import static com.rise_world.gematik.accesskeeper.server.api.util.ParameterUtils.validateDuplicateParameters;

@RestController
public class FedAuthEndpointImpl implements FedAuthEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(FedAuthEndpointImpl.class);

    private AuthorizationService authorizationService;
    private HttpServletRequest servletRequest;

    @Autowired
    public FedAuthEndpointImpl(AuthorizationService authorizationService, HttpServletRequest servletRequest) {
        this.authorizationService = authorizationService;
        this.servletRequest = servletRequest;
    }

    @Override
    // @AFO: A_23687 - idpIss wird an der Schnittstelle angenommen
    public Response startFedAuthorization(String idpIss, String responseType, String clientId, String state, String redirectUri, String scope,
                                          String codeChallenge, String codeChallengeMethod, String nonce) {
        // @AFO: A_20434 - keine doppelten Parameter
        validateDuplicateClientIdRedirectUri(servletRequest);
        authorizationService.validateClientAndRedirectUri(clientId, redirectUri);

        try {
            validateDuplicateParameters(servletRequest, ErrorCodes.AUTH_DUPLICATE_PARAMETERS);
            String sektorIdpAuthUrl = authorizationService.startFederatedAuthorization(idpIss, responseType, clientId, state, redirectUri, scope,
                codeChallenge, codeChallengeMethod, nonce);

            // @AFO: A_23688 - Authorization Request wird als HTTP redirect an den Aufrufer geschickt
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
    // @AFO: A_23691 - AUTHORIZATION_CODE_IDP wird an der Schnittstelle angenommen
    public Response finishFedAuthorization(String code, String state) {
        LOG.info("Received authcode for [state={}]", state);

        validateDuplicateParameters(servletRequest, ErrorCodes.AUTH_DUPLICATE_PARAMETERS);
        RedeemedChallengeDTO redeemedExtCode;

        redeemedExtCode = authorizationService.redeemFedAuthCode(code, state);

        String redirectUri = new UrlBuilder(redeemedExtCode.getRedirectUri())
            .appendParameter("code", redeemedExtCode.getAuthCode())
            .appendState(redeemedExtCode.getState())
            .toString();

        return Response.status(Response.Status.FOUND).location(URI.create(redirectUri)).build();
    }
}

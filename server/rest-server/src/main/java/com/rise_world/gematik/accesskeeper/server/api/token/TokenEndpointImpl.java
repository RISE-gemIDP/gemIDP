/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.api.token;

import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import com.rise_world.gematik.accesskeeper.common.util.LogTool.LogSeverity;
import com.rise_world.gematik.accesskeeper.server.dto.RedeemedTokenDTO;
import com.rise_world.gematik.accesskeeper.server.service.TokenService;
import com.rise_world.gematik.idp.server.api.token.TokenEndpoint;
import com.rise_world.gematik.idp.server.api.token.TokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

import static com.rise_world.gematik.accesskeeper.server.api.util.ParameterUtils.validateDuplicateParameters;

/**
 * Token Endpoint Implementierung
 *
 * @AFO A_20686 - Erweiterte Nutzung von Schlüsseln
 */
@RestController
public class TokenEndpointImpl implements TokenEndpoint {

    private static final String BEARER_TYPE = "Bearer";

    private TokenService service;
    private HttpServletRequest servletRequest;

    @Autowired
    public TokenEndpointImpl(TokenService service, HttpServletRequest servletRequest) {
        this.service = service;
        this.servletRequest = servletRequest;
    }

    /**
     * Redeem tokens
     * @AFO A_20323 - TOKEN-Ausgabe Protokollierung in allen Fällen (gemProdT_IDP-Dienst)
     * @AFO A_20321 - Annahme des Authorization Code und des Key_verifier
     */
    @Override
    public Response redeem(String authCode, String keyVerifier, String clientId, String grantType, String redirectUri) {
        // Protokollierung bei jedem Aufruf
        LogTool.logOperation(LogSeverity.INFO, "Token-Ausgabe wurde mittels Authorization Code angefordert");

        try {
            // @AFO: A_20434 - keine doppelten Parameter
            validateDuplicateParameters(servletRequest, ErrorCodes.TOKEN_DUPLICATE_PARAMETERS);
            RedeemedTokenDTO redeemToken = service.redeemToken(authCode, keyVerifier, clientId, grantType, redirectUri);
            LogTool.logOperation(LogSeverity.INFO, "Token-Ausgabe wurde f\u00fcr clientId {} durchgef\u00fchrt", clientId);
            return Response.ok(toTokenResponse(redeemToken)).build();
        }
        catch (Exception e) {
            // Protokollierung im Fehlerfall
            LogTool.logOperation(LogSeverity.ERROR, "Token-Ausgabe wurde aufgrund eines Problems abgebrochen", e);
            throw e;
        }
    }

    private TokenResponse toTokenResponse(RedeemedTokenDTO redeemToken) {
        TokenResponse response = new TokenResponse();
        response.setAccessToken(redeemToken.getAccessToken());
        response.setIdToken(redeemToken.getIdToken());
        response.setExpiresIn(redeemToken.getExpires());
        response.setTokenType(BEARER_TYPE);
        return response;
    }

}

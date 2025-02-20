/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import com.rise_world.gematik.accesskeeper.server.dto.EntityStatementDTO;
import com.rise_world.gematik.accesskeeper.server.dto.RequestSource;
import com.rise_world.gematik.accesskeeper.server.entity.ExtSessionEntity;
import com.rise_world.gematik.accesskeeper.server.util.PkceUtils;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.UnknownHostException;

@Service
public class ExtParService {

    private static final Logger LOG = LoggerFactory.getLogger(ExtParService.class);

    private static final int MAX_LENGTH_REQUEST_URI = 2000;


    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ConfigService configService;
    private final int maxRequestUriExpiry;
    private final PushedAuthClient client;

    public ExtParService(CircuitBreakerRegistry circuitBreakerRegistry,
                         ConfigService configService,
                         @Value("${federation.maxRequestUriExpiry:600}") int maxRequestUriExpiry,
                         PushedAuthClient pushedAuthClient) {

        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.configService = configService;
        this.maxRequestUriExpiry = maxRequestUriExpiry;
        this.client = pushedAuthClient;
    }

    /**
     * Send a PAR request to a remote IDP
     *
     * @param entityStatementDTO the entity statement of the remote IDP
     * @param sessionEntity      the persisted auth session parameters
     * @param appRedirectUri     the redirect_uri from the app request
     * @return the request uri from the PAR response
     */
    public String sendParRequest(EntityStatementDTO entityStatementDTO, ExtSessionEntity sessionEntity, String appRedirectUri) {
        LogTool.setPAREndpoint(entityStatementDTO.getPushedAuthorizationRequestEndpoint());
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(entityStatementDTO.getIssuer());

        String issuer = configService.getIssuer(RequestSource.INTERNET);

        PushedAuthResponse response;
        try {
            response = circuitBreaker.executeCallable(() -> client.send(URI.create(entityStatementDTO.getPushedAuthorizationRequestEndpoint()),
                issuer,
                sessionEntity.getState(),
                appRedirectUri,
                PkceUtils.createCodeChallenge(sessionEntity.getIdpCodeVerifier()),
                sessionEntity.getIdpNonce()
            ));
        }
        catch (CallNotPermittedException e) {
            LOG.warn("Remote IDP '{}' call was not permitted due to previous failures", entityStatementDTO.getIssuer());
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_IDP_NOT_AVAILABLE, e);
        }
        catch (ConnectTimeoutException | ConnectException | NoRouteToHostException | UnknownHostException e) {
            // handle timeout when remote service not available
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_PAR_FAILED, e);
        }
        catch (Exception e) {
            LOG.error("Unexpected error during PAR. Remote IDP: '{}'.", entityStatementDTO.getIssuer());
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_PAR_FAILED, e);
        }

        // @AFO: A_23688 -  Authorization Request als Client an den zugeh&ouml;rigen sektoralen Identity Provider
        validateParResponse(entityStatementDTO.getIssuer(), response);
        return response.parResponse().getRequestUri();
    }

    private void validateParResponse(String idpIss, PushedAuthResponse response) {

        var parResponse = response.parResponse();

        if ((response.status() == 400 || response.status() == 401)
            && ("invalid_client".equals(parResponse.getError()) || "unauthorized_client".equals(parResponse.getError()))) {
            LOG.error("PAR to {} failed with error {}:{}", idpIss, parResponse.getError(), parResponse.getErrorDescription());
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_IDP_NOT_REGISTERED);
        }
        if (response.status() != 201) {
            LOG.error("PAR to {} failed with error {}:{}", idpIss, parResponse.getError(), parResponse.getErrorDescription());
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_PAR_FAILED);
        }
        if (parResponse.getExpiresIn() == null || parResponse.getExpiresIn() <= 0 || parResponse.getExpiresIn() > maxRequestUriExpiry) {
            LOG.error("PAR to {} failed, invalid expires_in {}", idpIss, parResponse.getExpiresIn());
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_PAR_FAILED);
        }
        if (StringUtils.isBlank(parResponse.getRequestUri())) {
            LOG.error("PAR to {} failed, invalid request_uri {}", idpIss, parResponse.getRequestUri());
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_PAR_FAILED);
        }
        if (parResponse.getRequestUri().length() > MAX_LENGTH_REQUEST_URI) {
            LOG.error("PAR to {} failed, request_uri too long", idpIss);
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_PAR_FAILED);
        }
    }

}

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
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;

@Service
public class ExtAuthCodeService {

    private static final Logger LOG = LoggerFactory.getLogger(ExtAuthCodeService.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ConfigService configService;
    private final ExtAuthCodeClient client;

    @Autowired
    public ExtAuthCodeService(CircuitBreakerRegistry circuitBreakerRegistry,
                              ConfigService configService,
                              ExtAuthCodeClient client) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.configService = configService;
        this.client = client;
    }

    @SuppressWarnings("java:S2139")
    public String redeemFedAuthCode(EntityStatementDTO entityStatementDTO, String authorizationCode, String idpCodeVerifier, String clientRedirectUri) {
        LogTool.setTokenEndpoint(entityStatementDTO.getTokenEndpoint());
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(entityStatementDTO.getIssuer());

        String issuer = configService.getIssuer(RequestSource.INTERNET);

        try {
            var response = circuitBreaker.executeCallable(() -> client.send(
                URI.create(entityStatementDTO.getTokenEndpoint()),
                authorizationCode,
                idpCodeVerifier,
                issuer,
                clientRedirectUri));

            if (response.hasError()) {
                LOG.warn("Failed to read id token from remote IDP '{}'. Status: {}, Details: {}", entityStatementDTO.getIssuer(), response.getStatus(), response.getData());
                throw new AccessKeeperException(ErrorCodes.FEDAUTH_FAILED_TO_REDEEM);
            }

            return response.idToken();
        }
        catch (CallNotPermittedException e) {
            LOG.warn("Remote IDP '{}' call was not permitted due to previous failures", entityStatementDTO.getIssuer(), e);
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_IDP_NOT_AVAILABLE, e);
        }
        catch (SocketException | SocketTimeoutException | UnknownHostException e) {
            LOG.warn("Remote IDP '{}' is not available", entityStatementDTO.getIssuer(), e);
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_IDP_NOT_AVAILABLE, e);
        }
        catch (Exception e) {
            LOG.error("Unexpected error during auth code redemption. Remote IDP: '{}'.", entityStatementDTO.getIssuer(), e);
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_FAILED_TO_REDEEM, e);
        }
    }
}

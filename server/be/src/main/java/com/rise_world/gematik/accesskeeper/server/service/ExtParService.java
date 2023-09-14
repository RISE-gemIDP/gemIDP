/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.rise_world.gematik.accesskeeper.common.OAuth2Constants;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.service.PushedAuthEndpoint;
import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import com.rise_world.gematik.accesskeeper.common.util.LoggingInvocationHandler;
import com.rise_world.gematik.accesskeeper.common.util.TlsUtils;
import com.rise_world.gematik.accesskeeper.server.dto.EntityStatementDTO;
import com.rise_world.gematik.accesskeeper.server.dto.PARResponse;
import com.rise_world.gematik.accesskeeper.server.dto.RequestSource;
import com.rise_world.gematik.accesskeeper.server.entity.ExtSessionEntity;
import com.rise_world.gematik.accesskeeper.server.util.PkceUtils;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;

import static com.rise_world.gematik.accesskeeper.server.configuration.IdpConstants.USER_AGENT;

@Service
public class ExtParService {

    private static final Logger LOG = LoggerFactory.getLogger(ExtParService.class);

    private static final int MAX_LENGTH_REQUEST_URI = 2000;

    private JacksonJsonProvider jacksonJsonProvider;
    private SelfSignedCertificateService selfSignedCertificateService;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private ConfigService configService;
    private int sektorIdpConnectionTimeout;
    private int sektorIdpReceiveTimeout;
    private int maxRequestUriExpiry;

    @Autowired
    public ExtParService(JacksonJsonProvider jacksonJsonProvider,
                         SelfSignedCertificateService selfSignedCertificateService,
                         CircuitBreakerRegistry circuitBreakerRegistry,
                         ConfigService configService,
                         @Value("${sektorIdp.connection.timeout}") int sektorIdpConnectionTimeout,
                         @Value("${sektorIdp.token.receive.timeout}") int sektorIdpReceiveTimeout,
                         @Value("${federation.maxRequestUriExpiry:600}") int maxRequestUriExpiry) {
        this.jacksonJsonProvider = jacksonJsonProvider;
        this.selfSignedCertificateService = selfSignedCertificateService;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.configService = configService;
        this.sektorIdpConnectionTimeout = sektorIdpConnectionTimeout;
        this.sektorIdpReceiveTimeout = sektorIdpReceiveTimeout;
        this.maxRequestUriExpiry = maxRequestUriExpiry;
    }

    /**
     * Send a PAR request to a remote IDP
     *
     * @param entityStatementDTO the entity statement of the remote IDP
     * @param sessionEntity      the persisted auth session parameters
     * @return the request uri from the PAR response
     */
    public String sendParRequest(EntityStatementDTO entityStatementDTO, ExtSessionEntity sessionEntity) {
        LogTool.setPAREndpoint(entityStatementDTO.getPushedAuthorizationRequestEndpoint());
        PushedAuthEndpoint client = createClient(entityStatementDTO.getPushedAuthorizationRequestEndpoint());
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(entityStatementDTO.getIssuer());

        String issuer = configService.getIssuer(RequestSource.INTERNET);

        Response response;
        try {
            // @AFO: A_23687 - befüllen der Parameter laut Anforderung
            response = circuitBreaker.executeCallable(() -> client.pushedAuthorizationRequest(issuer,
                sessionEntity.getState(),
                issuer + "/fedauth",
                PkceUtils.createCodeChallenge(sessionEntity.getIdpCodeVerifier()),
                OAuth2Constants.PKCE_METHOD_S256,
                OAuth2Constants.RESPONSE_TYPE_CODE,
                sessionEntity.getIdpNonce(),
                "openid urn:telematik:display_name urn:telematik:versicherter",
                OAuth2Constants.ACR_LOA_HIGH,
                OAuth2Constants.CLIENT_ASSERTION_SELFSIGNED));
        }
        catch (CallNotPermittedException e) {
            LOG.warn("Remote IDP '{}' call was not permitted due to previous failures", entityStatementDTO.getIssuer());
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_IDP_NOT_AVAILABLE, e);
        }
        catch (ProcessingException e) {
            // handle timeout when remote service not available
            if ((e.getCause() instanceof SocketTimeoutException) || (e.getCause() instanceof ConnectException) ||
                (e.getCause() instanceof NoRouteToHostException) || (e.getCause() instanceof UnknownHostException)) {
                throw new AccessKeeperException(ErrorCodes.FEDAUTH_PAR_FAILED, e);
            }
            else {
                throw e;
            }
        }
        catch (Exception e) {
            LOG.error("Unexpected error during PAR. Remote IDP: '{}'.", entityStatementDTO.getIssuer());
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_PAR_FAILED, e);
        }
        try {
            // @AFO: A_23688 -  Authorization Request als Client an den zugeh&ouml;rigen sektoralen Identity Provider
            PARResponse parResponse = response.readEntity(PARResponse.class);
            validateParResponse(entityStatementDTO.getIssuer(), response, parResponse);
            return parResponse.getRequestUri();
        }
        catch (ProcessingException e) {
            LOG.warn("PAR to {} failed with error {}", entityStatementDTO.getIssuer(), response.readEntity(String.class));
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_PAR_FAILED, e);
        }
    }

    private void validateParResponse(String idpIss, Response response, PARResponse parResponse) {
        if ((response.getStatus() == 400 || response.getStatus() == 401)
            && ("invalid_client".equals(parResponse.getError()) || "unauthorized_client".equals(parResponse.getError()))) {
            LOG.error("PAR to {} failed with error {}:{}", idpIss, parResponse.getError(), parResponse.getErrorDescription());
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_IDP_NOT_REGISTERED);
        }
        else if (response.getStatus() != 201) {
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

    protected PushedAuthEndpoint createClient(String endpointUrl) {
        PushedAuthEndpoint parEndpoint = JAXRSClientFactory.create(endpointUrl, PushedAuthEndpoint.class,
            Collections.singletonList(jacksonJsonProvider), false);

        Client restClient = WebClient.client(parEndpoint);
        ClientConfiguration config = WebClient.getConfig(restClient);
        config.getResponseContext().put("buffer.proxy.response", Boolean.TRUE); // GEMIDP-1244 prevent connection leaks

        HTTPConduit conduit = config.getHttpConduit();
        conduit.setTlsClientParameters(TlsUtils.createTLSClientParameters());
        selfSignedCertificateService.secureWebClient(restClient);
        HTTPClientPolicy httpClientPolicy = conduit.getClient();
        httpClientPolicy.setBrowserType(USER_AGENT);
        // @AFO: A_23691 - Request Timeouts setzen
        httpClientPolicy.setConnectionTimeout(sektorIdpConnectionTimeout);
        httpClientPolicy.setReceiveTimeout(sektorIdpReceiveTimeout);

        return LoggingInvocationHandler.createLoggingProxy("PushedAuthEndpoint", PushedAuthEndpoint.class, parEndpoint);
    }
}

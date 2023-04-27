/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.rise_world.gematik.accesskeeper.common.OAuth2Constants;
import com.rise_world.gematik.accesskeeper.common.crypt.KeyConstants;
import com.rise_world.gematik.accesskeeper.common.crypt.SignatureProviderFactory;
import com.rise_world.gematik.accesskeeper.common.dto.Endpoint;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.util.LoggingInvocationHandler;
import com.rise_world.gematik.accesskeeper.common.util.RandomUtils;
import com.rise_world.gematik.accesskeeper.common.util.TlsUtils;
import com.rise_world.gematik.accesskeeper.server.dto.RemoteIdpDTO;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static com.rise_world.gematik.accesskeeper.server.configuration.IdpConstants.USER_AGENT;

@Service
public class ExtAuthCodeService {

    private static final Logger LOG = LoggerFactory.getLogger(ExtAuthCodeService.class);

    private Clock clock;
    private JacksonJsonProvider jacksonJsonProvider;
    private SignatureProviderFactory signatureProviderFactory;
    private int sektorIdpConnectionTimeout;
    private int sektorIdpReceiveTimeout;

    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    public ExtAuthCodeService(Clock clock, JacksonJsonProvider jacksonJsonProvider, SignatureProviderFactory signatureProviderFactory,
                              CircuitBreakerRegistry circuitBreakerRegistry,
                              @Value("${sektorIdp.connection.timeout}") int sektorIdpConnectionTimeout,
                              @Value("${sektorIdp.token.receive.timeout}") int sektorIdpReceiveTimeout) {
        this.clock = clock;
        this.jacksonJsonProvider = jacksonJsonProvider;
        this.signatureProviderFactory = signatureProviderFactory;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.sektorIdpConnectionTimeout = sektorIdpConnectionTimeout;
        this.sektorIdpReceiveTimeout = sektorIdpReceiveTimeout;

        LOG.debug("Using ExtTokenEndpoint client connection timeout to {} and receive timeout to {} ms", sektorIdpConnectionTimeout, sektorIdpReceiveTimeout);
    }

    // @AFO: A_22265 - Token Request an den sektoralen Identity Provider
    @SuppressWarnings("java:S2139") // log additional configuration information before throwing exceptions
    public String redeemAuthCode(RemoteIdpDTO remoteIdpDTO, String authCode, String redirectUri, String codeVerifier) {
        ExtTokenEndpoint tokenEndpoint = createClient(remoteIdpDTO.getTokenEndpoint());
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(remoteIdpDTO.getTokenEndpoint());

        String authToken = createAuthClaims(remoteIdpDTO);

        Map<String, String> response;
        try {
            response = circuitBreaker.executeCallable(() -> tokenEndpoint.redeem(authCode,
                codeVerifier,
                OAuth2Constants.EXTERNAL_CLIENT_ID,
                "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
                authToken,
                OAuth2Constants.GRANT_TYPE_CODE,
                redirectUri,
                null));
        }
        catch (WebApplicationException e) {
            final Response exceptionResponse = e.getResponse();
            if (LOG.isWarnEnabled()) {
                if (exceptionResponse.getMediaType() != null && exceptionResponse.getMediaType().toString().equals("application/json")) {
                    final Map<?, ?> exceptionJson = exceptionResponse.readEntity(Map.class);
                    LOG.warn("Failed to read id token from remote IDP '{}'. Details: {}", remoteIdpDTO.getAppConfig().getId(), exceptionJson, e);
                }
                else {
                    LOG.warn("Failed to read id token from remote IDP '{}'. Details: {}", remoteIdpDTO.getAppConfig().getId(), exceptionResponse.readEntity(String.class), e);
                }
            }
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_FAILED_TO_REDEEM, e);
        }
        catch (CallNotPermittedException e) {
            LOG.warn("Remote IDP '{}' call was not permitted due to previous failures", remoteIdpDTO.getAppConfig().getId(), e);
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_IDP_NOT_AVAILABLE, e);
        }
        catch (ProcessingException e) {
            if ((e.getCause() instanceof SocketTimeoutException) || (e.getCause() instanceof ConnectException) ||
                (e.getCause() instanceof NoRouteToHostException) || (e.getCause() instanceof UnknownHostException)) {
                LOG.warn("Remote IDP '{}' is not available", remoteIdpDTO.getAppConfig().getId(), e);
                throw new AccessKeeperException(ErrorCodes.EXTAUTH_IDP_NOT_AVAILABLE, e);
            }
            else {
                LOG.error("Unexpected error during auth code redemption. Remote IDP: '{}'.", remoteIdpDTO.getAppConfig().getId(), e);
                throw new AccessKeeperException(ErrorCodes.EXTAUTH_FAILED_TO_REDEEM, e);
            }
        }
        catch (Exception e) {
            LOG.error("Unexpected error during auth code redemption. Remote IDP: '{}'.", remoteIdpDTO.getAppConfig().getId(), e);
            throw new AccessKeeperException(ErrorCodes.EXTAUTH_FAILED_TO_REDEEM, e);
        }

        return response.get("id_token");
    }

    protected ExtTokenEndpoint createClient(String tokenEndpointUrl) {
        ExtTokenEndpoint tokenEndpoint = JAXRSClientFactory.create(tokenEndpointUrl, ExtTokenEndpoint.class,
            Collections.singletonList(jacksonJsonProvider), false);

        Client restClient = WebClient.client(tokenEndpoint);
        ClientConfiguration config = WebClient.getConfig(restClient);
        config.getResponseContext().put("buffer.proxy.response", Boolean.TRUE); // GEMIDP-1244 prevent connection leaks

        HTTPConduit conduit = config.getHttpConduit();
        conduit.setTlsClientParameters(TlsUtils.createTLSClientParameters());
        HTTPClientPolicy httpClientPolicy = conduit.getClient();
        httpClientPolicy.setBrowserType(USER_AGENT);
        httpClientPolicy.setConnectionTimeout(sektorIdpConnectionTimeout);
        httpClientPolicy.setReceiveTimeout(sektorIdpReceiveTimeout);

        return LoggingInvocationHandler.createLoggingProxy("ExtTokenEndpoint", ExtTokenEndpoint.class, tokenEndpoint);
    }

    // @AFO: A_22266 - private_key_jwt wird erstellt
    private String createAuthClaims(RemoteIdpDTO sektorIdp) {
        Instant now = clock.instant();
        long epochSecond = now.getEpochSecond();

        JwtClaims claims = new JwtClaims();
        claims.setIssuer(OAuth2Constants.EXTERNAL_CLIENT_ID);
        claims.setSubject(OAuth2Constants.EXTERNAL_CLIENT_ID);
        claims.setAudience(sektorIdp.getTokenEndpoint());
        claims.setTokenId(RandomUtils.randomUUID());
        claims.setIssuedAt(epochSecond);
        claims.setExpiryTime(epochSecond + 180); // @AFO: A_22266 - exp wird auf 3 Minuten gesetzt

        JwsHeaders headers = new JwsHeaders(SignatureAlgorithm.ES256);
        headers.setKeyId(KeyConstants.PUK_IDP_SIG_SEK);

        LOG.info("Created private_key_jwt [jti={}]", claims.getTokenId());
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(headers, claims);
        // @AFO: A_22266 - private_key_jwt wird mit PrK_IDP_SIG_Sek signiert
        return producer.signWith(signatureProviderFactory.createSignatureProvider(Endpoint.EXT_AUTH));
    }
}

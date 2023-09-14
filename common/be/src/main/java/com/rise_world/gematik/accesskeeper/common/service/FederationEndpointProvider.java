/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.service;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.rise_world.gematik.accesskeeper.common.util.LoggingInvocationHandler;
import com.rise_world.gematik.accesskeeper.common.util.TlsUtils;
import com.rise_world.gematik.idp.server.api.federation.FederationConfigurationEndpoint;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.stereotype.Service;

import java.util.Collections;


/**
 * Interface to provide access to a federation endpoint
 */
@Service
public class FederationEndpointProvider {

    private final JacksonJsonProvider jacksonJsonProvider;
    private final SynchronizationConfiguration configuration;

    public FederationEndpointProvider(JacksonJsonProvider jacksonJsonProvider, SynchronizationConfiguration configuration) {
        this.jacksonJsonProvider = jacksonJsonProvider;
        this.configuration = configuration;
    }

    /**
     * Creates a FederationConfigurationEndpoint client depending on the provided address
     *
     * @param endpointUrl representing the FederationConfigurationEndpoint
     * @param userAgent   HTTP User-Agent Header
     * @return an implementation of a FederationConfigurationEndpoint
     */
    public FederationConfigurationEndpoint create(String endpointUrl, String userAgent) {
        FederationConfigurationEndpoint endpoint = JAXRSClientFactory.create(endpointUrl, FederationConfigurationEndpoint.class,
            Collections.singletonList(jacksonJsonProvider), false);
        setupWebClient(userAgent, endpoint);
        return LoggingInvocationHandler.createLoggingProxy("FederationConfigurationEndpoint", FederationConfigurationEndpoint.class, endpoint);
    }

    /**
     * Create a web client for the specified endpoint url
     *
     * @param endpointUrl the endpoint url
     * @param userAgent   HTTP User-Agent Header
     * @return the created client
     */
    public SignedJwksEndpoint createJwksEndpoint(String endpointUrl, String userAgent) {
        SignedJwksEndpoint endpoint = JAXRSClientFactory.create(endpointUrl, SignedJwksEndpoint.class, Collections.singletonList(jacksonJsonProvider), false);
        setupWebClient(userAgent, endpoint);
        return LoggingInvocationHandler.createLoggingProxy("SignedJwksEndpoint", SignedJwksEndpoint.class, endpoint);
    }

    private void setupWebClient(String userAgent, Object endpoint) {
        Client restClient = WebClient.client(endpoint);
        ClientConfiguration config = WebClient.getConfig(restClient);
        config.getResponseContext().put("buffer.proxy.response", Boolean.TRUE); // GEMIDP-1244 prevent connection leaks

        HTTPConduit conduit = config.getHttpConduit();
        conduit.setTlsClientParameters(TlsUtils.createTLSClientParameters());
        HTTPClientPolicy httpClientPolicy = conduit.getClient();
        httpClientPolicy.setBrowserType(userAgent);
        httpClientPolicy.setConnectionTimeout(configuration.getConnectionTimeout().toMillis());
        httpClientPolicy.setReceiveTimeout(configuration.getReceiveTimeout().toMillis());
    }
}

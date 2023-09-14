/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.configuration;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.rise_world.gematik.accesskeeper.common.configuration.CxfTracingHeaderInterceptor;
import com.rise_world.gematik.accesskeeper.common.util.LoggingInvocationHandler;
import com.rise_world.gematik.idp.server.api.federation.FederationEndpoint;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class FederationMasterClientConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(FederationMasterClientConfiguration.class);

    @Bean
    public FederationEndpoint federationEndpoint(FederationMasterConfiguration configuration, CxfTracingHeaderInterceptor interceptor, JacksonJsonProvider jacksonJsonProvider) {
        FederationEndpoint endpoint = JAXRSClientFactory.create(
            configuration.getEndpoint(), FederationEndpoint.class, Collections.singletonList(jacksonJsonProvider), true);

        Client restClient = WebClient.client(endpoint);
        ClientConfiguration config = WebClient.getConfig(restClient);
        // GEMIDP-1244 prevent connection leaks
        config.getResponseContext().put("buffer.proxy.response", Boolean.TRUE);

        HTTPConduit conduit = config.getHttpConduit();
        HTTPClientPolicy httpClientPolicy = conduit.getClient();

        LOG.debug("Setting FedMaster client connection timeout to {} and receive timeout to {} ms",
            configuration.getConnectionTimeout(), configuration.getReceiveTimeout());
        httpClientPolicy.setConnectionTimeout(configuration.getConnectionTimeout());
        httpClientPolicy.setReceiveTimeout(configuration.getReceiveTimeout());

        WebClient.getConfig(endpoint).getOutInterceptors().add(interceptor);
        return LoggingInvocationHandler.createLoggingProxy("FederationMaster", FederationEndpoint.class, endpoint);
    }
}

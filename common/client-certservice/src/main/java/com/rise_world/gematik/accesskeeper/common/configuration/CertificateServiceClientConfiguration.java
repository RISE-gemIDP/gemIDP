/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.configuration;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.rise_world.epa.certificate.api.rest.api.CertificateNonQesApi;
import com.rise_world.gematik.accesskeeper.common.util.LoggingInvocationHandler;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class CertificateServiceClientConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateServiceClientConfiguration.class);

    @Bean
    public CertificateNonQesApi certificateResource(@Value("${certificateService.endpoint}") String certificateServiceApiUrl,
                                                    @Value("${certificateService.connection.timeout}") int certificateServiceConnectionTimeout,
                                                    @Value("${certificateService.receive.timeout}") int certificateServiceReceiveTimeout,
                                                    CxfTracingHeaderInterceptor interceptor,
                                                    JacksonJsonProvider jacksonJsonProvider) {
        CertificateNonQesApi certificateResource = JAXRSClientFactory.create(certificateServiceApiUrl, CertificateNonQesApi.class,
            Collections.singletonList(jacksonJsonProvider), true);

        Client restClient = WebClient.client(certificateResource);
        ClientConfiguration config = WebClient.getConfig(restClient);
        // GEMIDP-1244 prevent connection leaks
        config.getResponseContext().put("buffer.proxy.response", Boolean.TRUE);

        HTTPConduit conduit = config.getHttpConduit();
        HTTPClientPolicy httpClientPolicy = conduit.getClient();

        LOG.debug("Setting CertificateService client connection timeout to {} and receive timeout to {} ms",
            certificateServiceConnectionTimeout, certificateServiceReceiveTimeout);
        httpClientPolicy.setConnectionTimeout(certificateServiceConnectionTimeout);
        httpClientPolicy.setReceiveTimeout(certificateServiceReceiveTimeout);

        WebClient.getConfig(certificateResource).getOutInterceptors().add(interceptor);
        return LoggingInvocationHandler.createLoggingProxy("CertificateService", CertificateNonQesApi.class, certificateResource);
    }
}

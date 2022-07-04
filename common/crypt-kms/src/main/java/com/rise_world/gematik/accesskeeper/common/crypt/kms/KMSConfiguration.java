/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.crypt.kms;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.rise_world.gematik.accesskeeper.common.configuration.CxfTracingHeaderInterceptor;
import com.rise_world.gematik.accesskeeper.common.util.LoggingInvocationHandler;
import com.rise_world.gematik.idp.kms.api.rest.CertificateResource;
import com.rise_world.gematik.idp.kms.api.rest.PairingResource;
import com.rise_world.gematik.idp.kms.api.rest.TokenResource;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class KMSConfiguration {

    private String endpoint;
    @Value("${kms.receive.timeout:20000}")
    private long receiveTimeout;
    @Value("${kms.connection.timeout:10000}")
    private long connectionTimeout;

    public KMSConfiguration(@Value("${kms.endpoint}") String endpoint) {
        this.endpoint = endpoint;
    }

    @Bean
    public TokenResource tokenResource(JacksonJsonProvider provider, CxfTracingHeaderInterceptor interceptor) {
        final TokenResource tokenResource = JAXRSClientFactory.create(endpoint,
            TokenResource.class,
            Collections.singletonList(provider),
            true);

        updateClientConfig(tokenResource);
        WebClient.getConfig(tokenResource).getOutInterceptors().add(interceptor);

        return LoggingInvocationHandler.createLoggingProxy("KMS", TokenResource.class, tokenResource);
    }

    @Bean
    public CertificateResource getCertificateResource(JacksonJsonProvider provider, CxfTracingHeaderInterceptor interceptor) {
        final CertificateResource certificateResource = JAXRSClientFactory.create(endpoint,
            CertificateResource.class,
            Collections.singletonList(provider),
            true);

        updateClientConfig(certificateResource);
        WebClient.getConfig(certificateResource).getOutInterceptors().add(interceptor);

        return LoggingInvocationHandler.createLoggingProxy("KMS", CertificateResource.class, certificateResource);
    }

    @Bean
    public PairingResource getPairingResource(CxfTracingHeaderInterceptor interceptor) {
        final PairingResource pairingResource = JAXRSClientFactory.create(endpoint,
            PairingResource.class,
            Collections.emptyList(),
            true);

        updateClientConfig(pairingResource);
        WebClient.getConfig(pairingResource).getOutInterceptors().add(interceptor);

        return LoggingInvocationHandler.createLoggingProxy("KMS", PairingResource.class, pairingResource);
    }

    private void updateClientConfig(Object resource) {
        ClientConfiguration config = WebClient.getConfig(resource);
        // GEMIDP-1244 prevent connection leaks
        config.getResponseContext().put("buffer.proxy.response", Boolean.TRUE);

        HTTPClientPolicy policy = config.getHttpConduit().getClient();
        policy.setReceiveTimeout(receiveTimeout);
        policy.setConnectionTimeout(connectionTimeout);
    }
}

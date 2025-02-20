/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.service;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.rise_world.gematik.accesskeeper.common.util.LoggingInvocationHandler;
import com.rise_world.gematik.accesskeeper.common.util.TlsUtils;
import com.rise_world.gematik.idp.server.api.federation.FederationConfigurationEndpoint;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;


/**
 * Interface to provide access to a federation endpoint
 */
@Service
public class FederationEndpointProvider {

    private static final Logger LOG = LoggerFactory.getLogger(FederationEndpointProvider.class);

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
        URI uri = URI.create(endpointUrl);

        if (StringUtils.isNotBlank(uri.getPath())) {
            return create(uri.resolve("/").toString(), Optional.of(uri.getPath()), userAgent);
        }
        else {
            return create(endpointUrl, Optional.empty(), userAgent);
        }
    }

    /**
     * Creates a FederationConfigurationEndpoint client depending on the provided address and optional tenantId
     *
     * @param baseAddress representing the FederationConfigurationEndpoint
     * @param tenantId    optional tenantId that will be appended or prepended to the baseAddress
     * @param userAgent   HTTP User-Agent Header
     * @return an implementation of a FederationConfigurationEndpoint
     */
    protected FederationConfigurationEndpoint create(String baseAddress, Optional<String> tenantId, String userAgent) {
        MultiTenantFederationConfigurationEndpoint multiTenantEndpoint = JAXRSClientFactory.create(baseAddress, MultiTenantFederationConfigurationEndpoint.class,
            Collections.singletonList(jacksonJsonProvider), true);
        setupWebClient(userAgent, multiTenantEndpoint);
        MultiTenantFederationConfigurationEndpoint endpoint = LoggingInvocationHandler.createLoggingProxy("MultiTenantFederationConfigurationEndpoint",
            MultiTenantFederationConfigurationEndpoint.class, multiTenantEndpoint);

        return create(endpoint, tenantId);
    }

    /**
     * Creates a FederationConfigurationEndpoint client based on the provided endpoint and optional tenantId
     * <p>
     * If a tenantId is present, the client will first try to fetch the entity statement by appending the tenantId after path, e.g. /.well-known/openid-federation/{tenant-id},
     * if this fails, it will prepend the tenantId before /{tenant-id}/.well-known/openid-federation.
     * <p>
     * If no tenantId is present, it will use /.well-known/openid-federation
     *
     * @param endpoint representing the internal MultiTenantFederationConfigurationEndpoint
     * @param tenantId optional tenantId that will be appended or prepended to the baseAddress
     * @return an implementation of a FederationConfigurationEndpoint
     */
    protected FederationConfigurationEndpoint create(MultiTenantFederationConfigurationEndpoint endpoint, Optional<String> tenantId) {
        return () -> {
            if (tenantId.isPresent()) {
                try {
                    String federationEntity = endpoint.getFederationEntity(tenantId.get());
                    new JwsJwtCompactConsumer(federationEntity.trim()).getJwtClaims(); // trigger token parsing
                    return federationEntity;
                }
                catch (Exception e) {
                    LOG.warn("Could not fetch EntityStatement from default Endpoint, falling back", e);
                    return endpoint.getFederationEntityFromAlternativePath(tenantId.get());
                }
            }
            return endpoint.getFederationEntity();
        };
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

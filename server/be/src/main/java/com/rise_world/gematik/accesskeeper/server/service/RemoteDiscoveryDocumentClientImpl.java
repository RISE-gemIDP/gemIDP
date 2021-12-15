/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.rise_world.gematik.accesskeeper.server.dto.RemoteIdpDTO;
import com.rise_world.gematik.accesskeeper.server.model.SektorApp;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jwk.KeyType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class RemoteDiscoveryDocumentClientImpl implements RemoteDiscoveryDocumentClient {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteDiscoveryDocumentClientImpl.class);

    private JacksonJsonProvider jacksonJsonProvider;
    private int connectionTimeout;
    private int receiveTimeout;

    @Autowired
    public RemoteDiscoveryDocumentClientImpl(JacksonJsonProvider jacksonJsonProvider,
                                             @Value("${sektorIdp.connection.timeout}") int connectionTimeout,
                                             @Value("${sektorIdp.dd.receive.timeout}") int receiveTimeout) {
        this.jacksonJsonProvider = jacksonJsonProvider;
        this.connectionTimeout = connectionTimeout;
        this.receiveTimeout = receiveTimeout;
    }

    @Override
    public Optional<RemoteIdpDTO> loadDiscoveryDocument(SektorApp idp) {
        final String discoveryDocumentUrl = getDiscoveryDocumentUrl(idp);
        try {
            LOG.info("DD for '{}' will be loaded from '{}'", idp.getId(), discoveryDocumentUrl);

            WebClient webClient = createWebClient(discoveryDocumentUrl);
            setConnectionTimeouts(webClient);

            final Map<?, ?> response = webClient.get(Map.class);

            assertEntries(response, "issuer", "token_endpoint", "jwks_uri");
            String issuer = response.get("issuer").toString();
            String tokenEndpoint = response.get("token_endpoint").toString();
            String jwksUri = response.get("jwks_uri").toString();

            validateEndpointUrl(tokenEndpoint);
            validateEndpointUrl(jwksUri);

            RemoteIdpDTO remoteIdp = new RemoteIdpDTO(Clock.systemUTC().instant(), idp, issuer, tokenEndpoint);

            webClient = createWebClient(jwksUri);
            String string = webClient.get(String.class);
            List<JsonWebKey> jwks = JwkUtils.readJwkSet(string).getKeys(); // returns null in case of an empty list...

            if (jwks != null) {
                jwks.forEach(key -> {
                    PublicKey publicKey = toPublicKey(key);
                    if (publicKey != null) {
                        remoteIdp.getWebKeys().put(key.getKeyId(), publicKey);
                    }
                });
            }

            LOG.info("DD for '{}' was success fully loaded from '{}'", idp.getId(), discoveryDocumentUrl);
            return Optional.of(remoteIdp);
        }
        catch (Exception e) {
            LOG.error("Failed to retrieve DD for '{}' from '{}'", idp.getId(), discoveryDocumentUrl, e);
            return Optional.empty();
        }
    }

    /**
     * Create a web client for the specified endpoint url
     *
     * @param endpointUrl the endpoint url
     * @return the created client
     */
    protected WebClient createWebClient(String endpointUrl) {
        return WebClient.create(endpointUrl, Collections.singletonList(jacksonJsonProvider));
    }

    private void setConnectionTimeouts(WebClient client) {
        ClientConfiguration configuration = client.getConfiguration();
        HTTPClientPolicy clientPolicy = configuration.getHttpConduit().getClient();
        clientPolicy.setConnectionTimeout(connectionTimeout);
        clientPolicy.setReceiveTimeout(receiveTimeout);
    }

    private String getDiscoveryDocumentUrl(SektorApp s) {
        StringBuilder sb = new StringBuilder(s.getIdpIss().length() + 35);
        sb.append(s.getIdpIss());
        if (!s.getIdpIss().endsWith("/")) {
            sb.append('/');
        }
        sb.append(".well-known/openid-configuration");
        return sb.toString();
    }

    private PublicKey toPublicKey(JsonWebKey jwk) {
        try {
            if (jwk.getKeyType() == KeyType.RSA) {
                return JwkUtils.toRSAPublicKey(jwk);
            }
            else if (jwk.getKeyType() == KeyType.EC) {
                return JwkUtils.toECPublicKey(jwk);
            }
        }
        catch (Exception e) {
            LOG.warn("Failed to parse JsonWebKey [type={}], [kid={}]", jwk.getKeyType(), jwk.getKeyId());
        }

        return null;
    }

    private void assertEntries(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Validate.isTrue(map.containsKey(key), String.format("DD does not contain entry %s", key));
        }
    }

    private void validateEndpointUrl(String url) throws MalformedURLException {
        URL endpointUrl = new URL(url);
        Validate.isTrue("https".equals(endpointUrl.getProtocol()));
    }
}

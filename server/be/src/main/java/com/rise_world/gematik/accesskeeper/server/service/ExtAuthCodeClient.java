/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.common.OAuth2Constants;
import com.rise_world.gematik.accesskeeper.server.http.AccessKeeperHttpClient;
import com.rise_world.gematik.accesskeeper.server.http.AccessKeeperHttpClientConfig;
import com.rise_world.gematik.accesskeeper.server.http.AccessKeeperHttpClientFactory;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class ExtAuthCodeClient {

    private static final Logger LOG = LoggerFactory.getLogger(ExtAuthCodeClient.class);

    private final AccessKeeperHttpClient client;
    private final ExtAuthCodeResponseHandler responseHandler;

    ExtAuthCodeClient(AccessKeeperHttpClientFactory clientFactory,
                      @Value("${sektorIdp.connection.timeout}") int sektorIdpConnectionTimeout,
                      @Value("${sektorIdp.token.receive.timeout}") int sektorIdpReceiveTimeout,
                      @Value("${sektorIdp.token.request.timeout}") int requestTimeout,
                      ExtAuthCodeResponseHandler responseHandler) {
        LOG.debug("Using ExtTokenEndpoint client connection timeout to {} and receive timeout to {} ms", sektorIdpConnectionTimeout, sektorIdpReceiveTimeout);

        client = clientFactory.create(new AccessKeeperHttpClientConfig("ExtTokenEndpoint",
            Duration.of(requestTimeout, ChronoUnit.MILLIS),
            Duration.of(sektorIdpConnectionTimeout, ChronoUnit.MILLIS),
            Duration.of(sektorIdpReceiveTimeout, ChronoUnit.MILLIS)));

        this.responseHandler = responseHandler;
    }

    // @AFO: A_22265 - Abbildung des Tokenendpunkts des sektoralen IDPs laut OAuth2 Spezifikation
    // @AFO: A_23691-01 - befüllen der Parameter laut Anforderung
    public ExtAuthCodeResponse send(URI uri, String authCode, String codeVerifier, String clientId, String redirectUri) throws IOException {
        var request = new HttpPost(uri);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
        request.setEntity(new UrlEncodedFormEntity(List.of(
            new BasicNameValuePair("code", authCode),
            new BasicNameValuePair("code_verifier", codeVerifier),
            new BasicNameValuePair("client_id", clientId),
            new BasicNameValuePair("client_assertion_type", OAuth2Constants.CLIENT_ASSERTION_SELFSIGNED),
            new BasicNameValuePair("client_assertion", null),
            new BasicNameValuePair("grant_type", OAuth2Constants.GRANT_TYPE_CODE),
            new BasicNameValuePair("redirect_uri", redirectUri),
            new BasicNameValuePair("refresh_token", null)
        )));

        var response = client.execute(request, "redeem", responseHandler);
        return new ExtAuthCodeResponse(response.getStatus(), response.getData().orElseGet(Map::of));
    }
}

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PushedAuthClient {

    private final AccessKeeperHttpClient client;
    private final PushedAuthResponseHandler responseHandler;

    PushedAuthClient(AccessKeeperHttpClientFactory httpClientFactory,
                     @Value("${sektorIdp.connection.timeout}") int sektorIdpConnectionTimeout,
                     @Value("${sektorIdp.par.receive.timeout}") int sektorIdpReceiveTimeout,
                     @Value("${sektorIdp.par.request.timeout}") int requestTimeout,
                     PushedAuthResponseHandler responseHandler) {
        client = httpClientFactory.create(new AccessKeeperHttpClientConfig("PushedAuthEndpoint",
            Duration.of(requestTimeout, ChronoUnit.MILLIS),
            Duration.of(sektorIdpConnectionTimeout, ChronoUnit.MILLIS),
            Duration.of(sektorIdpReceiveTimeout, ChronoUnit.MILLIS)));

        this.responseHandler = responseHandler;
    }

    // @AFO: A_23687 - befüllen der Parameter laut Anforderung
    PushedAuthResponse send(URI uri,
                            String clientId,
                            String state,
                            String redirectURI,
                            String codeChallenge,
                            String nonce) throws IOException {

        var request = new HttpPost(uri);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
        request.setEntity(new UrlEncodedFormEntity(List.of(
            new BasicNameValuePair("client_id", clientId),
            new BasicNameValuePair("state", state),
            new BasicNameValuePair("redirect_uri", redirectURI),
            new BasicNameValuePair("code_challenge", codeChallenge),
            new BasicNameValuePair("code_challenge_method", OAuth2Constants.PKCE_METHOD_S256),
            new BasicNameValuePair("response_type", OAuth2Constants.RESPONSE_TYPE_CODE),
            new BasicNameValuePair("nonce", nonce),
            new BasicNameValuePair("scope", "openid urn:telematik:display_name urn:telematik:versicherter"),
            new BasicNameValuePair("acr_values", OAuth2Constants.ACR_LOA_HIGH)
        )));

        var result = client.execute(request, "pushedAuthorizationRequest", responseHandler);
        return new PushedAuthResponse(result.getStatus(), result.getData().orElse(null));
    }

}

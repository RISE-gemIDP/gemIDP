/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.http;

import com.rise_world.gematik.accesskeeper.common.util.TlsUtils;
import com.rise_world.gematik.accesskeeper.server.service.SelfSignedCertificateService;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static com.rise_world.gematik.accesskeeper.server.configuration.IdpConstants.USER_AGENT;
import static java.util.Objects.isNull;
import static net.logstash.logback.marker.Markers.append;

public class AccessKeeperHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(AccessKeeperHttpClient.class);

    private final SelfSignedCertificateService certificateService;
    private final AccessKeeperHttpClientConfig config;

    private final HttpRequestMonitor requestMonitor;

    /**
     * lazy initialisation of the http client to ensure tls setup
     * is done correctly using SelfSignedCertificateService
     */
    private HttpClient http;

    AccessKeeperHttpClient(SelfSignedCertificateService certificateService, HttpRequestMonitor requestMonitor, AccessKeeperHttpClientConfig config) {
        this.certificateService = certificateService;
        this.requestMonitor = requestMonitor;
        this.config = config;
    }

    public <T> HttpResult<T> execute(HttpUriRequestBase request,
                                     String method,
                                     ResponseHandler<T> responseHandler) throws IOException {
        if (!request.containsHeader("User-Agent")) {
            request.addHeader("User-Agent", USER_AGENT);
        }

        var task = requestMonitor.monitor(request);

        if (LOG.isDebugEnabled()) {
            LOG.debug("{} remote call start method={}", config.system(), method);
        }

        var success = true;
        var watch = StopWatch.createStarted();

        try {
            return httpClient().execute(request, responseHandler::handle);
        }
        catch (IOException e) {
            success = false;
            throw e;
        }
        finally {
            watch.stop();

            task.cancel(false);

            if (LOG.isInfoEnabled()) {
                double durationMs = watch.getNanoTime() / 1000000.0;
                LOG.info(append("component", config.system())
                        .and(append("method", method))
                        .and(append("duration_in_ms_float", durationMs))
                        .and(append("successful", success)),
                    "{} remote call done method={} duration={}ms successful={}", config.system(), method, formatMillis(durationMs), success);
            }
        }
    }

    private static String formatMillis(double millis) {
        return new DecimalFormat("#.###", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(millis);
    }

    private HttpClient httpClient() {
        if (isNull(http)) {
            http = initHttpClient();
        }

        return http;
    }

    private HttpClient initHttpClient() {
        SSLContext sslContext;
        try {

            sslContext = SSLContext.getInstance(TLS.V_1_2.getId());
            sslContext.init(certificateService.getKeyManagers(), certificateService.getTrustManagers(), new SecureRandom());
        }
        catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException("unable to configure http client", e);
        }

        var tlsConfig = TlsUtils.tlsConfig();

        var connectionManager = new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
            .register("https", new SSLConnectionSocketFactory(sslContext.getSocketFactory(),
                tlsConfig.getSupportedProtocols(),
                tlsConfig.getSupportedCipherSuites(),
                new DefaultHostnameVerifier()))
            .build());

        connectionManager.setDefaultTlsConfig(tlsConfig);

        connectionManager.setDefaultConnectionConfig(ConnectionConfig.custom()
            .setConnectTimeout(Timeout.of(config.connectTimeout()))
            .setSocketTimeout(Timeout.of(config.reciveTimeout()))
            .build());

        return HttpClients.custom()
            .disableAutomaticRetries()
            .disableCookieManagement()
            .disableDefaultUserAgent()
            .disableRedirectHandling()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(RequestConfig.custom()
                .setResponseTimeout(Timeout.of(config.reciveTimeout()))
                .build())
            .build();
    }

}

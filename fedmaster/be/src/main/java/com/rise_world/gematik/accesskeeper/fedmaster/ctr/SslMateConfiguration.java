/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

import com.rise_world.gematik.accesskeeper.fedmaster.util.HttpsEnforcer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "federation.ctr.sslmate")
public class SslMateConfiguration {

    @DurationUnit(ChronoUnit.MILLIS)
    private Duration connectionTimeout;

    @DurationUnit(ChronoUnit.MILLIS)
    private Duration receiveTimeout;

    private String apiKey;
    private String endpoint;

    private int pageLimit;

    private List<CtrProviderRateLimit> limits = List.of();

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Duration getReceiveTimeout() {
        return receiveTimeout;
    }

    public void setReceiveTimeout(Duration receiveTimeout) {
        this.receiveTimeout = receiveTimeout;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = enforceHttps(endpoint);
    }

    public int getPageLimit() {
        return pageLimit;
    }

    public void setPageLimit(int pageLimit) {
        this.pageLimit = pageLimit;
    }

    public List<CtrProviderRateLimit> getLimits() {
        return limits;
    }

    public void setLimits(List<CtrProviderRateLimit> limits) {
        this.limits = limits;
    }

    private static String enforceHttps(String endpoint) {

        if (!HttpsEnforcer.isHttps(endpoint)) {
            throw new IllegalArgumentException("sslmate.endpoint must use https");
        }

        return endpoint;
    }
}

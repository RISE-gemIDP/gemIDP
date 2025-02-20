/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Configuration
@ConfigurationProperties(prefix = "federation.ctr.crtsh")
public class CrtShConfiguration {

    @DurationUnit(ChronoUnit.MILLIS)
    private Duration connectionTimeout;

    @DurationUnit(ChronoUnit.MILLIS)
    private Duration receiveTimeout;

    private String endpoint;

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

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = enforceHttps(endpoint);
    }

    private static String enforceHttps(String endpoint) {

        if (!Objects.equals(URI.create(endpoint).getScheme(), "https")) {
            throw new IllegalArgumentException("crtsh.endpoint must use https");
        }

        return endpoint;
    }
}

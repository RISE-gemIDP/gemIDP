/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
@ConfigurationProperties(prefix = "federation.synchronization")
public class SynchronizationConfiguration {

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration expiration;

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration maxDowntime;

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration interval;

    @DurationUnit(ChronoUnit.MILLIS)
    private Duration connectionTimeout;

    @DurationUnit(ChronoUnit.MILLIS)
    private Duration receiveTimeout;

    public Duration getExpiration() {
        return expiration;
    }

    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
    }

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

    public Duration getMaxDowntime() {
        return maxDowntime;
    }

    public void setMaxDowntime(Duration maxDowntime) {
        this.maxDowntime = maxDowntime;
    }

    public Duration getInterval() {
        return interval;
    }

    public void setInterval(Duration interval) {
        this.interval = interval;
    }
}

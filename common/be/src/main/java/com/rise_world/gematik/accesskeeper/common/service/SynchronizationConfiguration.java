/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static java.util.Objects.nonNull;

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

    private LockFeature lock;

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

    /**
     * {@code lockRelyingParty} returns {@code true} if the application is configured
     * to lock participants of type {@code openid_relying_party} if the registration data
     * does not match the data in the entity statement
     *
     * @return {@code true} if participant lock is configured, {@code false} otherwise
     */
    public boolean lockRelyingParty() {
        return nonNull(lock) && lock.isRelyingParty();
    }

    public void setLock(LockFeature lock) {
        this.lock = lock;
    }

    public static class LockFeature {

        private boolean relyingParty;

        public boolean isRelyingParty() {
            return relyingParty;
        }

        public void setRelyingParty(boolean relyingParty) {
            this.relyingParty = relyingParty;
        }
    }
}

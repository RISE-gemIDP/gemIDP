/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * {@code CtrProviderRateLimit} holds the configuration for a single
 * rate limit.
 * <p>
 * {@link #capacity} represents the number of requests allowed per {@link #period}
 * the default value for {@link #type} is {@link CtrProviderRateLimitType#WAIT_AND_CONTINUE}
 */
public class CtrProviderRateLimit {

    private int capacity;

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration period;

    private CtrProviderRateLimitType type = CtrProviderRateLimitType.WAIT_AND_CONTINUE;

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public Duration getPeriod() {
        return period;
    }

    public void setPeriod(Duration period) {
        this.period = period;
    }

    public CtrProviderRateLimitType getType() {
        return type;
    }

    public void setType(CtrProviderRateLimitType type) {
        this.type = type;
    }
}

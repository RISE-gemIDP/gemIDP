/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;

/**
 * {@code SslMateRateLimiter} uses the {@link SslMateConfiguration#getLimits()} to stay within the rate limits
 * configured for SSL Mate
 */
@Service
public class SslMateRateLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(SslMateRateLimiter.class);

    private final RateLimitWaitHandler waitHandler;
    private final Clock clock;

    private final List<CtrProviderRateLimit> limits;
    private final boolean limiterEnabled;

    private final List<Instant> requests;

    public SslMateRateLimiter(RateLimitWaitHandler waitHandler, Clock clock, SslMateConfiguration config) {
        this.waitHandler = waitHandler;
        this.clock = clock;

        // sort limits. descending from longest to shortest interval
        limits = config.getLimits()
            .stream()
            .sorted(comparing(CtrProviderRateLimit::getPeriod).reversed())
            .toList();

        limiterEnabled = !limits.isEmpty();

        requests = new ArrayList<>();
    }

    /**
     * {@code acquire} checks if the request is allowed or if any of the configured {@link CtrProviderRateLimit limits} is exceeded.
     * <p>
     * if the exceeded {@link CtrProviderRateLimit limit} is of type {@link CtrProviderRateLimitType#TERMINATE} a {@link RequestLimitExceededException}
     * will be thrown otherwise this method blocks until the next request is allowed.
     */
    public void acquire() {
        if (!limiterEnabled) {
            return;
        }

        var now = clock.instant();

        var maxLimit = limits.get(0);
        cleanupExpiredRequests(now, maxLimit.getPeriod());

        getWaitTime(now).ifPresent(waitHandler::sleep);

        requests.add(now);
    }

    private void cleanupExpiredRequests(Instant now, Duration maxLimit) {
        var timeout = now.minus(maxLimit);
        requests.removeIf(request -> request.isBefore(timeout));
    }

    /**
     * To avoid waiting in a loop, {@code getWaitTime} calculates the wait time based on the configured {@link CtrProviderRateLimit limits},
     * processing from the longest period to the shortest.
     * <p>
     * To ensure we do not run into rate limits caused by discrepancies between client and server algorithms
     * the capacity is reduced by 1 when calculating the position of the first request in the time window (aka we are too fast)
     *
     * @param now {@link Instant timestamp} of the current request
     * @return wait {@link Duration duration} before executing the next request or {@link Optional#empty()} if no limit was exceeded
     */
    private Optional<Duration> getWaitTime(Instant now) {

        for (var limit : limits) {

            if (requests.size() < limit.getCapacity()) {
                continue;
            }

            if (limit.getType() == CtrProviderRateLimitType.TERMINATE) {
                throw new RequestLimitExceededException("request limit of %d requests per %s exceeded".formatted(limit.getCapacity(), limit.getPeriod()));
            }

            var firstRequest = requests.get(requests.size() - (limit.getCapacity() - 1));
            var timeToWait = Duration.between(now, firstRequest.plus(limit.getPeriod()));
            if (limitExceeded(timeToWait)) {
                LOG.info("rate limit {}/{} exceeded, waiting for {}", limit.getCapacity(), limit.getPeriod(), timeToWait);
                return Optional.of(timeToWait);
            }
        }

        return Optional.empty();
    }

    private static boolean limitExceeded(Duration timeToWait) {
        return !timeToWait.isZero() && !timeToWait.isNegative();
    }

    List<Instant> requests() {
        return requests;
    }
}

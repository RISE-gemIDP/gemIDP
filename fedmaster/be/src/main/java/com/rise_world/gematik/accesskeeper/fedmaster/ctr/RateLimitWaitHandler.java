/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitWaitHandler {

    /**
     * {@code sleep} waits for the given {@link Duration}
     *
     * @param duration {@link Duration} to wait
     */
    public void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

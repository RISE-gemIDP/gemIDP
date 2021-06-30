/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

import java.time.Clock;

@Configuration
public class ClockProducer {

    /**
     * Produces a clock instance
     *
     * @return the produced instance
     */
    @Bean
    @RequestScope
    public Clock produceClock() {
        return Clock.systemUTC();
    }
}

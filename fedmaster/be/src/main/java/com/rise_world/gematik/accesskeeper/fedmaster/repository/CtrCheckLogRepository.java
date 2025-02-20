/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.repository;

import com.rise_world.gematik.accesskeeper.fedmaster.schedule.CTRProvider;

import java.time.Instant;
import java.util.Optional;

/**
 * {@code CtrCheckLogRepository} to access CtrCheckLog data
 */
public interface CtrCheckLogRepository {

    /**
     * {@code saveSuccess} saves a successful run for the given {@code provider} at the given {@code timestamp}
     *
     * @param provider  {@link CTRProvider} successful checked provider
     * @param timestamp {@link Instant} monitoring timestamp
     */
    void saveSuccess(CTRProvider provider, Instant timestamp);

    /**
     * {@code lastSuccess} returns the timestamp of the last successful monitoring run for the given {@link CTRProvider provider}
     * if no (successful) monitoring run was executed for the given {@link CTRProvider provider} an {@code empty} {@link Optional} will be returned.
     *
     * @param ctrProvider {@link CTRProvider}
     * @return {@link Optional} containing the timestamp of the last successful monitoring run or {@link Optional#empty() empty} if no
     * successful monitoring run exists
     */
    Optional<Instant> lastSuccess(CTRProvider ctrProvider);
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.repository;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import com.rise_world.gematik.accesskeeper.fedmaster.schedule.CTRProvider;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Repository to access federation participants
 */
public interface ParticipantRepository {

    /**
     * Fetch all active identifiers known by the federation master
     *
     * @return a list of entity identifiers
     */
    List<String> findAllIdentifiers();

    /**
     * Fetch a {@link ParticipantDto participant} by its primary key.
     * The {@link ParticipantDto} must be {@link ParticipantDto#isActive() active}
     *
     * @param id primary key of the entity
     * @return {@code active} {@link ParticipantDto participant} or an empty {@link Optional}
     */
    Optional<ParticipantDto> findById(Long id);

    /**
     * {@code findByIdWithInactive} loads the {@link ParticipantDto participant} with the given {@code id}.
     *
     * @param id primary key of the entity
     * @return {@link ParticipantDto participant} with the given {@code id} or {@link Optional#empty()}
     */
    Optional<ParticipantDto> findByIdWithInactive(Long id);

    /**
     * Fetch an entity by the known entity identifier
     *
     * @param identifier of the expected entity
     * @return participant
     */
    Optional<ParticipantDto> findByIdentifier(String identifier);

    /**
     * Fetch all active entities of type openid_provider known by the federation master
     *
     * @return a list of openid provider
     */
    List<ParticipantDto> findAllOpenIdProviders();

    /**
     * Fetch all active participants with a synchronizedAt timestamp before the provided date
     *
     * @param beforeSync synchronization date
     * @return a list of participants with a synchronization date older than the provided date
     */
    List<ParticipantDto> findBeforeSyncAt(Date beforeSync);

    /**
     * {@code findBeforeSyncAtWithInactive} returns all {@link ParticipantDto participants}
     * with a synchronizedAt timestamp before the provided {@code beforeSync}
     *
     * @param beforeSync synchronization date
     * @return {@link ParticipantDto participants} with a synchronization date older than {@code beforeSync}
     */
    List<ParticipantDto> findBeforeSyncAtWithInactive(Date beforeSync);

    /**
     * Updates a participant and sets synchronizationAt to the current timestamp
     *
     * @param participant to be updated
     * @param sync        time of synchronization
     */
    void synchronizeParticipant(ParticipantDto participant, Timestamp sync);

    /**
     * Updates a participant and sets lastScheduledRun to the current timestamp
     *
     * @param identifier to be updated
     * @param run        time of last scheduled run
     */
    void setLastRun(Long identifier, Timestamp run);

    /**
     * Updates a participant and sets lastMonitoredAt to the current timestamp
     *
     * @param identifier to be updated
     * @param monitoring time of last scheduled run
     * @param provider monitoring {@link CTRProvider}
     */
    void setMonitoringRun(Long identifier, Timestamp monitoring, CTRProvider provider);

    /**
     * Fetches all {@link ParticipantDto participants} of type {@link com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantType#OP openid_provider}
     * with a {@code lastMonitored} timestamp before the provided date for the given {@link CTRProvider}
     *
     * @param monitoringTime monitoring timestamp
     * @param provider       monitoring {@link CTRProvider provider}
     * @return list of {@link ParticipantDto openid-providers} with outdated certificate transparency check
     */
    List<ParticipantDto> findBeforeMonitoredAt(Instant monitoringTime, CTRProvider provider);

    /**
     * {@code setActive} updates the {@link ParticipantDto#isActive() active flag} of the
     * {@link ParticipantDto participant} with the given {@code id}
     *
     * @param id the primary key of the {@link ParticipantDto participant} to update
     * @param active the new state
     */
    void setActive(Long id, boolean active);

}

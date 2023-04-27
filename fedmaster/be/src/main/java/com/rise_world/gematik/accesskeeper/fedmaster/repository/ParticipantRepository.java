/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.repository;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;

import java.sql.Timestamp;
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
     * Fetch an entity by its primary key
     *
     * @param id primary key of the entity
     * @return participant
     */
    Optional<ParticipantDto> findById(Long id);

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
     * Fetch all active participants with a synchronizedAt timestamp or a lastMonitored timestamp
     * before the provided date
     *
     * @param beforeSync        synchronization date
     * @param beforeMonitoring  monitoring date
     * @return a list of participants with a synchronization date older than the provided date
     */
    List<ParticipantDto> findBeforeSyncAt(Date beforeSync, Date beforeMonitoring);

    /**
     * Updates a participant and sets synchronizationAt to the current timestamp
     * @param participant to be updated
     * @param sync time of synchronization
     */
    void synchronizeParticipant(ParticipantDto participant, Timestamp sync);

    /**
     * Updates a participant and sets lastScheduledRun to the current timestamp
     * @param identifier to be updated
     * @param run time of last scheduled run
     */
    void setLastRun(Long identifier, Timestamp run);

    /**
     * Updates a participant and sets lastMonitoredAt to the current timestamp
     * @param identifier to be updated
     * @param monitoring time of last scheduled run
     */
    void setMonitoringRun(Long identifier, Timestamp monitoring);
}

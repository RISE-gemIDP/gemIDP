/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.repository;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDomainDto;

import java.util.List;

/**
 * Repository to access openid provider domains
 */
public interface DomainRepository {

    /**
     * Fetches all domains associated to a participant
     *
     * @param identifier of the participant
     * @return a list of domains
     */
    List<ParticipantDomainDto> findByParticipant(Long identifier);

    /**
     * Removes a domain from the repository
     *
     * @param identifier to be removed
     */
    void delete(Long identifier);

    /**
     * Saves a domain to the repository
     *
     * @param domain to be saved
     */
    void save(ParticipantDomainDto domain);
}

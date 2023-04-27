/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.repository;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.CertificatePublicKeyDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantKeyDto;

import java.util.List;
import java.util.Optional;

/**
 * Repository to access public keys of federation participants
 */
public interface PublicKeyRepository {

    /**
     * Fetch all certificate public keys associated to an entity
     *
     * @param participantId primary key of an entity
     * @return a list of all associated certificate public keys
     */
    List<CertificatePublicKeyDto> findAllCertificateKeysByParticipant(Long participantId);

    /**
     * Fetch all entity statement signature keys associated to an entity
     *
     * @param participantId primary key of an entity
     * @return a list of all associated entity statement signature keys
     */
    List<ParticipantKeyDto> findByParticipant(Long participantId);

    /**
     * Fetch a participant entity statement signature key for a distinct participant by the provided key identifier
     *
     * @param participantId participant identifier
     * @param kid key identifier
     * @return found entity statement signature key or empty optional
     */
    Optional<ParticipantKeyDto> findKeyByParticipantAndKeyId(Long participantId, String kid);
}


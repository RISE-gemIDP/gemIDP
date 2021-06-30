/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.repository;

import com.rise_world.gematik.accesskeeper.pairingdienst.entity.PairingEntryEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Pairings.
 */
public interface PairingRepository {

    /**
     * Stores the pairing.
     *
     * @param pairing the pairing to be stored
     * @return id of the new pairing
     */
    long save(PairingEntryEntity pairing);

    /**
     * Loads the pairing with the given idNummer and keyIdentifier from the repository.
     *
     * @param idNummer      idNummer
     * @param keyIdentifier keyIdentifier
     * @return the pairing or {@code Optional.empty()} if no such pairing exists
     */
    Optional<PairingEntryEntity> fetchPairing(String idNummer, String keyIdentifier);

    /**
     * Fetches all pairings associated to the {@code idNummer}.
     *
     * @param idNummer idNummer
     * @return list of pairings (can be empty)
     */
    List<PairingEntryEntity> fetchPairings(String idNummer);

    /**
     * Deletes the pairing associated to both the idNummer the
     * {@code keyIdentifier}.
     *
     * @param idNummer      idNummer
     * @param keyIdentifier keyIdentifier
     * @return {@code true} if the pairing was deleted, {@code false} if no pairing
     *         was deleted
     */
    boolean deletePairing(String idNummer, String keyIdentifier);
}

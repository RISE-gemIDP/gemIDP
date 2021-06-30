/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.service;

import com.rise_world.gematik.accesskeeper.pairingdienst.dto.AccessTokenDTO;
import com.rise_world.gematik.accesskeeper.pairingdienst.entity.PairingEntryEntity;
import com.rise_world.gematik.accesskeeper.pairingdienst.exception.PairingDienstException;

import java.util.List;

/**
 * Provides business logic for handling pairings.
 * <p>
 * See {@link PairingEntryEntity} for a definition of the term "Pairing".
 */
public interface PairingService {

    /**
     * Registers a new pairing.
     * <p>
     * The provided {@code encryptedRegistrationData} is decrypted and validated. On success a new pairing is created and stored.
     *
     * @param accessToken               token to authorize the registration request
     * @param encryptedRegistrationData encrypted registration data (a JWE)
     * @return the new pairing
     */
    PairingEntryEntity registerPairing(AccessTokenDTO accessToken, String encryptedRegistrationData);

    /**
     * Fetches all pairings associated to the idNummer in {@code accessToken}.
     *
     * @param accessToken token to authorize this inspection request
     * @return list of pairings (can be empty)
     */
    List<PairingEntryEntity> inspectPairings(AccessTokenDTO accessToken);

    /**
     * Deletes the pairing associated to both the idNummer in {@code accessToken}
     * and the {@code keyIdentifier}.
     *
     * @param accessToken   token to authorize this inspection request
     * @param keyIdentifier keyIdentifier
     * @throws PairingDienstException if no such pairing exists
     */
    void deregisterPairing(AccessTokenDTO accessToken, String keyIdentifier);

    /**
     * Verifies that an "alternative authentication" is permissible.
     * <p>
     * Checks that the provided {@code signedAuthenticationData} is valid, comes from an eligible (not blocked) device,
     * and corresponds to a registered, not expired pairing.
     *
     * @param signedAuthenticationData signed authentication data (a JWS)
     * @return challenge token extracted from authentication data
     * @throws PairingDienstException if verification fails, i.e. authentication is not permissible
     */
    String verifyAlternativeAuthentication(String signedAuthenticationData);
}

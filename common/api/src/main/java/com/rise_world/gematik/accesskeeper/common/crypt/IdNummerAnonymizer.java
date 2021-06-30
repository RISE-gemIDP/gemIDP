/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.crypt;

/**
 * Anonymizes idNummers.
 * <p>
 * The anonymized idNummer is used as primary key of {@code PairingEntryEntity}.
 */
public interface IdNummerAnonymizer {

    /**
     * Anonymizes the given idNummer.
     *
     * @param idNummer idNummer
     * @return base64url-encoded string
     */
    String anonymizeIdNummer(String idNummer);
}

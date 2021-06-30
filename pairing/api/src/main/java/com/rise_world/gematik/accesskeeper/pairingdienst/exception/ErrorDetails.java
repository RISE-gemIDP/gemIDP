/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.exception;

public enum ErrorDetails {
    ID_NUMMER_MISMATCH,
    INVALID_AUTH_CERT,
    INVALID_SIGNED_PAIRING_DATA,
    INVALID_SIGNATURE_SIGNED_AUTHENTICATION_DATA,
    INVALID_CONTENT,

    INVALID_HEADER,
    OCSP_CHECK_FAILED,
    PAIRING_ENTRY_NOT_FOUND,
    DATABASE_INCONSISTENT,
    BLOCKED_DEVICE
}

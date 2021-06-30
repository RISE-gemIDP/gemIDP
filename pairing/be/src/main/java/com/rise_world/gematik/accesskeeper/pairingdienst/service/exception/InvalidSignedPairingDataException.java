/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.service.exception;

public class InvalidSignedPairingDataException extends RuntimeException {

    public InvalidSignedPairingDataException() {
    }

    public InvalidSignedPairingDataException(String message) {
        super(message);
    }

    public InvalidSignedPairingDataException(Throwable cause) {
        super(cause);
    }

    public InvalidSignedPairingDataException(String message, Throwable cause) {
        super(message, cause);
    }
}

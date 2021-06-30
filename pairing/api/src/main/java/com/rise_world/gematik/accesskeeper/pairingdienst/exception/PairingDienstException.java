/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.exception;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;

public class PairingDienstException extends AccessKeeperException {

    private final ErrorDetails errorDetails;

    public PairingDienstException(ErrorMessage errorMessage, ErrorDetails errorDetails) {
        super(errorMessage);
        this.errorDetails = errorDetails;
    }

    public PairingDienstException(ErrorMessage errorMessage) {
        this(errorMessage, (ErrorDetails) null);
    }

    public PairingDienstException(ErrorMessage errorMessage, ErrorDetails errorDetails, Throwable cause) {
        super(errorMessage, cause);
        this.errorDetails = errorDetails;
    }

    public PairingDienstException(ErrorMessage errorMessage, Throwable cause) {
        this(errorMessage, null, cause);
    }

    public ErrorDetails getErrorDetails() {
        return errorDetails;
    }
}

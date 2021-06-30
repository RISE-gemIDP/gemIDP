/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.exception;

public class AccessKeeperException extends RuntimeException {

    private final ErrorMessage errorMessage;

    public AccessKeeperException(ErrorMessage errorMessage) {
        super(errorMessage.getText());
        this.errorMessage = errorMessage;
    }

    public AccessKeeperException(ErrorMessage errorMessage, Throwable t) {
        super(errorMessage.getText(), t);
        this.errorMessage = errorMessage;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }
}

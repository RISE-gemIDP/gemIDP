/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.exception;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;

public class FederationMasterException extends AccessKeeperException {

    private final FederationMasterOperation operation;

    public FederationMasterException(ErrorMessage errorMessage, FederationMasterOperation operation) {
        super(errorMessage);
        this.operation = operation;
    }

    public FederationMasterException(ErrorMessage errorMessage, FederationMasterOperation operation, Throwable t) {
        super(errorMessage, t);
        this.operation = operation;
    }

    public FederationMasterOperation getOperation() {
        return operation;
    }
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.exception;

public class EntityStatementSyncException extends RuntimeException {

    public EntityStatementSyncException(String message) {
        super(message);
    }

    public EntityStatementSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}

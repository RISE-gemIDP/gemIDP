/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

import static com.rise_world.gematik.accesskeeper.fedmaster.service.Severity.ERROR;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.Severity.INFO;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.Severity.WARN;

public enum StatusCode {

    OK(0, INFO),

    // status codes entity statement synchronization
    NOT_REACHABLE(100, WARN),
    MAX_DOWNTIME_REACHED(101, ERROR),
    INVALID_SUB(102, ERROR),
    KID_UNKNOWN(200, ERROR),
    SIGNATURE_INVALID(201, ERROR),
    UNSUPPORTED_SIGNATURE_ALGORITHM(202, ERROR),
    TOKEN_INVALID(300, ERROR),
    REGISTRATION_DATA_INVALID(301, ERROR),
    TECHNICAL(400, ERROR),

    // status codes certificate transparency monitoring
    NO_CERT(500, ERROR),
    UNKNOWN_KEY(501, ERROR),
    REJECTED(502, ERROR),
    SERVICE_ERROR(510, ERROR),
    ;

    private final int code;
    private final Severity severity;

    StatusCode(int error, Severity severity) {
        this.code = error;
        this.severity = severity;
    }

    public int getCode() {
        return this.code;
    }

    public Severity getSeverity() {
        return severity;
    }

}

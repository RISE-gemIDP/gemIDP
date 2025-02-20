/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

import java.util.Collections;
import java.util.Map;

public class SynchronizationException extends Exception {

    private final StatusCode code;
    private final Map<String, String> related;

    public SynchronizationException(StatusCode code, String msg) {
        this(code, Collections.emptyMap(), msg);
    }

    public SynchronizationException(StatusCode code, Map<String, String> related, String msg) {
        super(msg);
        this.code = code;
        this.related = related;
    }

    public SynchronizationException(StatusCode code, String msg, Throwable reason) {
        this(code, Collections.emptyMap(), msg, reason);
    }

    public SynchronizationException(StatusCode code, Map<String, String> related, String msg, Throwable reason) {
        super(msg, reason);
        this.code = code;
        this.related = related;
    }

    public StatusCode getStatusCode() {
        return code;
    }

    public Map<String, String> getRelated() {
        return related;
    }
}

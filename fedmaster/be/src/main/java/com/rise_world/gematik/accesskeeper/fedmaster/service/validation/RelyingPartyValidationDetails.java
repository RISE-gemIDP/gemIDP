/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service.validation;

import java.util.Map;

public class RelyingPartyValidationDetails {

    private final String message;
    private final Map<String, String> related;

    public RelyingPartyValidationDetails(String message, Map<String, String> related) {
        this.message = message;
        this.related = related;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getRelated() {
        return related;
    }
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.dto;

import java.util.Map;

public class UserConsentDTO {

    private Map<String, String> requestedScopes;
    private Map<String, String> requestedClaims;

    public UserConsentDTO(Map<String, String> requestedScopes, Map<String, String> requestedClaims) {
        this.requestedScopes = requestedScopes;
        this.requestedClaims = requestedClaims;
    }

    public Map<String, String> getRequestedScopes() {
        return requestedScopes;
    }

    public void setRequestedScopes(Map<String, String> requestedScopes) {
        this.requestedScopes = requestedScopes;
    }

    public Map<String, String> getRequestedClaims() {
        return requestedClaims;
    }

    public void setRequestedClaims(Map<String, String> requestedClaims) {
        this.requestedClaims = requestedClaims;
    }
}

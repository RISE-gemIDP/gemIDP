/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.dto;

public enum RequestSource {

    TI("ti"),
    INTERNET("internet"),
    ;

    private final String code;

    RequestSource(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static RequestSource getByCode(String code) {
        for (RequestSource entry : values()) {
            if (entry.code.equals(code)) {
                return entry;
            }
        }
        throw new IllegalArgumentException("Unknown request source");
    }
}

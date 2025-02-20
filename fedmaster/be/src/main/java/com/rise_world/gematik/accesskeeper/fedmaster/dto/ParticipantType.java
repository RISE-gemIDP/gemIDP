/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.dto;

import java.util.stream.Stream;

public enum ParticipantType {

    // openid_relying_party
    RP("openid_relying_party"),
    // openid_provider
    OP("openid_provider");

    private final String type;

    ParticipantType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static ParticipantType getByType(String code) {
        return Stream.of(values())
            .filter(type -> type.type.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown participant type " + code));
    }
}

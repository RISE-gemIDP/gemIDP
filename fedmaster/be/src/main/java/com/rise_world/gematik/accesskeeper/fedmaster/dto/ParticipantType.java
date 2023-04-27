/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.dto;

import org.apache.cxf.rs.security.jose.jwt.JwtClaims;

import java.util.Collections;

public enum ParticipantType {

    // openid_relying_party
    RP("openid_relying_party", new JwtClaims()
        .setClaim("client_registration_types", Collections.singletonList("automatic"))
    ),
    // openid_provider
    OP("openid_provider", new JwtClaims()
        .setClaim("client_registration_types_supported", Collections.singletonList("automatic"))
    );

    private final String type;
    private final JwtClaims typeClaims;

    ParticipantType(String type, JwtClaims typeClaims) {
        this.type = type;
        this.typeClaims = typeClaims;
    }

    public String getType() {
        return type;
    }

    public JwtClaims createDefaultMetadata() {
        return new JwtClaims().setClaim(type, typeClaims);
    }

    public static ParticipantType getByType(String code) {
        for (ParticipantType entry : values()) {
            if (entry.type.equals(code)) {
                return entry;
            }
        }
        throw new IllegalArgumentException("Unknown type");
    }
}

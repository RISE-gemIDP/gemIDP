/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;

public class JwtJsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JwtJsonUtils() {
    }

    public static String serializeClaims(JwtClaims claims) {
        try {
            return MAPPER.writeValueAsString(claims.asMap());
        }
        catch (JsonProcessingException e) {
            return ExceptionUtils.rethrow(e);
        }
    }
}

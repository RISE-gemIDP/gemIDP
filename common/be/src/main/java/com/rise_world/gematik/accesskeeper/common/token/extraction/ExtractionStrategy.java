/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.token.extraction;

import java.util.Collections;
import java.util.Map;

/**
 * Strategy to extract relevant claims from a token
 */
public interface ExtractionStrategy<T> {

    /**
     * Extracts and validates the claims of a provided token
     * @param token where claims will be extracted and validated
     * @return extracted claims
     */
    default T extractAndValidate(String token) {
        return extractAndValidate(token, Collections.emptyMap());
    }

    /**
     * Extracts and validates the claims of a provided token
     *
     * @param token where claims will be extracted and validated
     * @param context additional context information that may be used for extract and validate
     * @return extracted claims
     */
    T extractAndValidate(String token, Map<String, Object> context);
}

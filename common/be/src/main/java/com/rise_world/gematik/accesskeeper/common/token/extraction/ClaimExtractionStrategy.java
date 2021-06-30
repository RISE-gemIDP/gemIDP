/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.token.extraction;

import org.apache.cxf.rs.security.jose.jwt.JwtClaims;

/**
 * Strategy to extract relevant claims from a token
 */
public interface ClaimExtractionStrategy extends ExtractionStrategy<JwtClaims> {

    /**
     * Extracts and validates the claims of a provided token
     * @param token where claims will be extracted and validated
     * @return extracted claims
     */
    JwtClaims extractAndValidate(String token);
}

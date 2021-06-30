/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.service;

import org.apache.cxf.rs.security.jose.jwt.JwtClaims;

/**
 * Parses an IDP access token
 */
public interface AccessTokenParser {

    /**
     * Extracts and validates the claims the access token
     * @param token where claims will be extracted and validated
     * @return extracted claims
     */
    JwtClaims extractAndValidate(String token);
}

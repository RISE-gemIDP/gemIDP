/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.creation;

import org.apache.cxf.rs.security.jose.jwt.JwtClaims;

/**
 * Strategy for token creation
 */
public interface TokenCreationStrategy {

    /**
     * Creates a token based on the provided claims
     * @param claims to be represented in the token
     * @return assembled token
     */
    String toToken(JwtClaims claims);
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.creation;

import org.apache.cxf.rs.security.jose.jwt.JwtClaims;

import javax.crypto.SecretKey;

/**
 *  Strategy for token creation that encrypts the result with a provided AES key
 */
public interface AesTokenCreationStrategy {

    /**
     * Creates a token based on the provided claims and the AES key used for the content encryption
     *
     * @param claims to be represented in the token
     * @param key    the base64 encoded AES key
     * @return assembled token
     */
    String toToken(JwtClaims claims, SecretKey key);
}

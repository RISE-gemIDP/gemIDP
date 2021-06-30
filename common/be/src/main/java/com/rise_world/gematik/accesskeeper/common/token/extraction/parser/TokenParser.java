/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.token.extraction.parser;

import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;

/**
 * Implementations of this interface parse a token into a {@link JwsJwtCompactConsumer}
 */
public interface TokenParser {

    /**
     * Parse/process the token and return a consumer containing header, body and signature
     *
     * @param token the token
     * @return the parsed token
     */
    IdpJwsJwtCompactConsumer parse(String token);
}

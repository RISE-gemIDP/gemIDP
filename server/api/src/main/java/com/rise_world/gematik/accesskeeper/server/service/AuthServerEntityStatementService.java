/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

/**
 * Service providing entity statement objects about the authorization server
 */
public interface AuthServerEntityStatementService {

    /**
     * Create the entity statement of the authorization server. The entity statement
     * will be returned as JWT in compact serialization format as described by RFC 7515.
     *
     * @return entity statement for the authorization server
     */
    String createEntityStatement();
}

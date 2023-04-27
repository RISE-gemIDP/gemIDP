/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

import java.util.List;

/**
 * Service providing entity statement objects about the federation participants
 */
public interface EntityStatementService {

    /**
     * Fetch the entity statement of the federation master. The entity statement
     * will be returned as JWT in compact serialization format as described by RFC 7515.
     *
     * @return entity statement for the federation master
     */
    String fetchMasterEntityStatement();

    /**
     * Fetch the entity statement of the requested participant of the federation. The
     * entity statement will be returned as JWT in compact serialization format as
     * described by RFC 7515.
     * <p>
     * The iss parameter must be set.
     * <p>
     * The other parameters may be null.
     * <p>
     * The iss parameter must be the same value as configured for the instance of the federation master.
     * In case the sub parameter is not set, the configured value of the federation master will be used.
     * In case the aud parameter is not set, no aud claim will be set in the entity statement.
     *
     * @param iss expected issuer
     * @param sub expected subject of the entity statement
     * @param aud expected audience of the entity statement
     * @return entity statement of the federation participant
     */
    String fetchEntityStatement(String iss, String sub, String aud);

    /**
     * Fetch all entity identifiers registered as participants of the federation.
     *
     * @return a list of all entity identifiers
     */
    List<String> getSubEntityIds();

    /**
     * Fetch all participants of type openid_provider and provide this list as JWT in compact
     * serialization format as described by RFC 7515.
     *
     * @return list of openid providers registered in the federation
     */
    String getIdpList();
}

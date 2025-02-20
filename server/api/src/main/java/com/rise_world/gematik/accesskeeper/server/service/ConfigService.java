/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;


import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
import com.rise_world.gematik.accesskeeper.server.dto.RequestSource;
import com.rise_world.gematik.accesskeeper.server.model.Client;
import com.rise_world.gematik.accesskeeper.server.model.Fachdienst;
import com.rise_world.gematik.accesskeeper.server.model.InfoModel;
import com.rise_world.gematik.accesskeeper.server.model.Scope;

import java.util.List;
import java.util.Set;

/**
 * Provides methods for reading the accesskeeper configuration
 */
public interface ConfigService {

    /**
     * Reloads the infomodel configuration file
     */
    void reload();

    /**
     * Get the absolute location of the infomodel json file
     *
     * @return infomodel localation
     */
    String getConfigLocation();

    /**
     * The effective infomodel (doesn't contain invalid clients / fachdienste)
     *
     * @return the effective infomodel
     */
    InfoModel getEffectiveInfoModel();

    /**
     * The parsed info model (contains ALL entries)
     *
     * @return the parsed infomodel
     */
    InfoModel getParsedInfoModel();

    /**
     * The configured token issuer
     *
     * @param requestSource ti or internet
     * @return the configured token issuer
     */
    String getIssuer(RequestSource requestSource);

    /**
     * The configured token issuers (TI and internet)
     *
     * @return the set of configured issuer identifiers
     */
    Set<String> getIssuers();

    /**
     * The configured eRp authserver client name
     *
     * @return the configured client name
     */
    String getAuthServerClientName();

    /**
     * The configured eRp authserver organization name
     *
     * @return the configured organization name
     */
    String getAuthServerOrganizationName();

    /**
     * The pairing endpoint (the public endpoint!)
     *
     * @return the configured pairing endpoint
     */
    String getPairingEndpoint();

    /**
     * The salt value used for generating the sub claim.
     * <p>
     * As defined here: https://openid.net/specs/openid-connect-core-1_0.html#PairwiseAlg
     *
     * @return the configured salt
     */
    String getSalt();

    /**
     * Get the timeout for all non-FD tokens (CHALLENGE, ACCESS_CODE, ID, SSO)
     *
     * @param tokenType the type of the token
     * @return the configured timeout value in seconds
     * @throws IllegalArgumentException if called with a FD specific token type (e.g. ACCESS)
     */
    Long getTokenTimeout(TokenType tokenType);

    /**
     * Get a scope by id
     *
     * @param scopeId the unique scope id
     * @return the scope or {@code null}
     */
    Scope getScopeById(String scopeId);

    /**
     * Get a client by id
     *
     * @param clientId the unique client id
     * @return the client or {@code null}
     */
    Client getClientById(String clientId);

    /**
     * Returns the ids of all clients which failed client validation
     *
     * @return the invalid ids
     */
    Set<String> getInvalidClientIds();

    /**
     * Returns the ids of all fachdienst entries which failed fachdienst validation
     *
     * @return the invalid ids
     */
    Set<String> getInvalidFachdienstIds();

    /**
     * Returns the ids of all scope entries which failed scope validation
     *
     * @return the invalid ids
     */
    Set<String> getInvalidScopeIds();

    /**
     * Returns the ids of all sektor idp entries which failed validation
     *
     * @return the invalid ids
     */
    Set<String> getInvalidSektorAppIds();

    /**
     * A gematik scope is mapped to exactly one fachdienst
     *
     * @param scopeId the unique scope id
     * @return the fachdienst, that requires the scope or {@code null}
     */
    Fachdienst getFachdienstByScope(String scopeId);

    /**
     * Returns all configured scopes that map to a fachdienst
     *
     * @return the list of fachdienst scope ids
     */
    List<String> getFachdienstScopes();

    /**
     * Returns all validRedirectUris that should be present in the entity statement of the AuthServer.
     *
     * @return the set of redirect uris
     */
    Set<String> getRedirectUrisForEntityStatement();
}

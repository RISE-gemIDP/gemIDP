/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.server.dto.OpenidProviderDTO;

import java.util.List;

/**
 * Provides methods for retrieving registered identity providers supporting identity federation
 */
public interface IdentityFederationDirectoryService {

    /**
     * Return list of remote IDPs as signed JWT
     * <p>
     * The claim 'fed_idp_list' contains the list of registered IDPs.
     *
     * @return the signed JWT
     */
    String getRemoteIdps();

    /**
     * Return a list of remote IDPs
     *
     * @return a list of {@link OpenidProviderDTO}
     */
    List<OpenidProviderDTO> getOpenIdProviders();
}

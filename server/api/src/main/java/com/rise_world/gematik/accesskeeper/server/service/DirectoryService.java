/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.server.dto.RemoteIdpDTO;

/**
 * Provides methods for retrieving registered identity providers
 */
public interface DirectoryService {

    /**
     * Return the application directory as signed JWT
     * <p>
     * The claim 'kk_app_list' contains the list of registered applications.
     *
     * @return the signed JWT
     */
    String getAppDirectory();

    /**
     * Returns the remote IDP associated with the provided application ID
     *
     * @param kkAppId the id of the mobile application
     * @return the configuration of a remote IDP
     * @throws com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException if {@code kkAppid} is {@code null}, if the application is unknown
     *                                                                                    or if downloading the remote discovery document failed
     */
    RemoteIdpDTO getRemoteIdpConfig(String kkAppId);

    /**
     * Rebuild the the directory cache. <b>All</b> existing entries will be purged!
     */
    void rebuildDirectoryCache();
}

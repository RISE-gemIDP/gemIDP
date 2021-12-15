/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.server.dto.RemoteIdpDTO;
import com.rise_world.gematik.accesskeeper.server.model.SektorApp;

import java.util.Optional;

/**
 * Client for downloading and parsing remote discovery documents
 */
public interface RemoteDiscoveryDocumentClient {

    /**
     * Read the discovery document for this sektor app
     * @param idp the sektor app
     * @return the parsed DD configuration or empty in case of an error
     */
    Optional<RemoteIdpDTO> loadDiscoveryDocument(SektorApp idp);
}

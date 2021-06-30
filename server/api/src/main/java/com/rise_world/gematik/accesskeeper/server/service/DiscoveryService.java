/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.server.dto.DiscoveryDocumentType;

/**
 * Provides methods for retrieving and updating the discovery document
 */
public interface DiscoveryService {

    /**
     * Creates the discovery document
     *
     * @param type the type of the requested document
     * @return Discovery Document in form of JWS Compact Serialization
     */
    String getDiscoverDocument(DiscoveryDocumentType type);

}

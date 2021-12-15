/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

/**
 * Provides methods for retrieving and updating the discovery document
 */
public interface DiscoveryService {

    /**
     * Creates the discovery document
     *
     * @return Discovery Document in form of JWS Compact Serialization
     */
    String getDiscoverDocument();

}

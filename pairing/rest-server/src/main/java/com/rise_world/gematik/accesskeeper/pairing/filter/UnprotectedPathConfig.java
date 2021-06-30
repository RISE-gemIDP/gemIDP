/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairing.filter;

import java.util.List;

/**
 * Defines the list of paths that are accessible without login (e.g. health check)
 */
public interface UnprotectedPathConfig {

    /**
     * Get the list of unprotected paths
     *
     * @return the list of unprotected request paths
     */
    List<String> getUnprotectedPaths();
}

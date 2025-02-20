/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.http;

import java.time.Duration;

public record AccessKeeperHttpClientConfig(String system,
                                           Duration requestTimeout,
                                           Duration connectTimeout,
                                           Duration reciveTimeout) {
}

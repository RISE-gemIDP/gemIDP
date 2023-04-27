/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.dto;

import java.time.Instant;
import java.util.List;

public record CtRecordDto(String id,
                          List<String> dnsNames,
                          String issuerName,
                          String pubKeyHash,
                          Instant notBefore,
                          Instant notAfter,
                          boolean revoked) {
}

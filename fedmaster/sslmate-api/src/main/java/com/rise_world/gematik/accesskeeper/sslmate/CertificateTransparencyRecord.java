/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.sslmate;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record CertificateTransparencyRecord(
    String id,
    @JsonProperty("tbs_sha256") String tbsHash,
    @JsonProperty("dns_names") List<String> dnsNames,
    Issuer issuer,
    @JsonProperty("pubkey_sha256") String pubKeyHash,
    @JsonProperty("not_before") Instant notBefore,
    @JsonProperty("not_after") Instant notAfter,
    boolean revoked) {
}

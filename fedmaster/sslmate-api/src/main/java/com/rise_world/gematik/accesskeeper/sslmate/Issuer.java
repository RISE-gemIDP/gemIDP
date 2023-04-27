/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.sslmate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Issuer(
    String name,
    @JsonProperty("pubkey_sha256") String pubKeyHash
) {
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.crtsh;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class CrtShRecord {

    private final String id;
    private final String issuer;
    private final Instant notBefore;
    private final Instant notAfter;

    @JsonCreator
    public CrtShRecord(
        @JsonProperty("id") String id,
        @JsonProperty("issuer_name") String issuer,
        @JsonProperty("not_before") String notBefore,
        @JsonProperty("not_after") String notAfter) {

        this.id = id;
        this.issuer = issuer;
        this.notBefore = parse(notBefore);
        this.notAfter = parse(notAfter);
    }

    public String getId() {
        return id;
    }

    public String getIssuer() {
        return issuer;
    }

    public Instant getNotBefore() {
        return notBefore;
    }

    public Instant getNotAfter() {
        return notAfter;
    }

    private static Instant parse(String value) {
        return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
    }
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.dto;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ParticipantClaims {

    private final Set<String> claims;

    public ParticipantClaims(List<String> claims) {
        this.claims = Set.copyOf(claims);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ParticipantClaims that)) {
            return false;
        }

        return claims.equals(that.claims);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(claims);
    }

    @Override
    public String toString() {
        return claims.toString();
    }

    public List<String> asList() {
        return List.copyOf(claims);
    }

    public boolean hasAllClaims(ParticipantClaims fetchedClaims) {
        return claims.containsAll(fetchedClaims.claims);
    }
}

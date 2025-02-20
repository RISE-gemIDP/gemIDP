/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.dto;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ParticipantRedirectUris {

    private final Set<String> uris;

    public ParticipantRedirectUris(List<String> uris) {
        this.uris = Set.copyOf(uris);
    }

    public List<String> asList() {
        return List.copyOf(uris);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ParticipantRedirectUris that = (ParticipantRedirectUris) o;
        return uris.equals(that.uris);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uris);
    }

    @Override
    public String toString() {
        return uris.toString();
    }
}

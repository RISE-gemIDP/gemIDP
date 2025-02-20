/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.dto;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ParticipantScopes {

    private final Set<String> scopes;

    public ParticipantScopes(List<String> scopes) {
        this.scopes = Set.copyOf(scopes);
    }

    public ParticipantScopes(String... scopes) {
        this.scopes = Set.of(scopes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ParticipantScopes that)) {
            return false;
        }

        return Objects.equals(scopes, that.scopes);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(scopes);
    }

    /**
     * {@code asString} returns a string representation of the scopes, with each scope separated by a space.
     *
     * @return a space-separated string of scopes.
     */
    public String asString() {
        return String.join(" ", scopes);
    }

    /**
     * {@code hasAllScopes} determines if all scopes from the provided {@link ParticipantScopes} are allowed.
     * Empty provided {@code fetchedScopes} are treated as valid.
     *
     * @param fetchedScopes the {@link ParticipantScopes} to check against.
     * @return {@code true} if this instance contains all scopes from {@code fetchedScopes},
     * {@code false} otherwise.
     */
    public boolean hasAllScopes(ParticipantScopes fetchedScopes) {
        return fetchedScopes.scopes.isEmpty() || scopes.containsAll(fetchedScopes.scopes);
    }
}

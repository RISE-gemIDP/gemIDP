/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.schedule;

import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Objects.isNull;

public enum CTRProvider {

    SSL_MATE("sslmate"),
    CRT_SH("crtsh");

    private final String id;

    CTRProvider(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return getId();
    }

    /**
     * byId returns the {@link CTRProvider} with the given {@code id}, or {@code null} if the given {@code id} is {@code null}.
     * If the {@code id} is not {@code null} and no {@link CTRProvider} with the {@code id} exists, an {@link IllegalArgumentException} will be thrown
     *
     * @param id (nullable) id of the {@link CTRProvider}
     * @return {@link CTRProvider} with the given {@code id} or {@code null} of the {@code id} is null
     * @throws IllegalArgumentException if no {@link  CTRProvider} exists for the given {@code id}
     */
    public static CTRProvider byId(String id) {
        if (isNull(id)) {
            return null;
        }

        return Stream.of(values())
                .filter(provider -> Objects.equals(provider.id, id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown CTR provider: " + id));
    }
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.http;

import java.util.Optional;

/**
 * {@code HttpResult} provides the {@link #getStatus() status code} and an optional response body of type {@code T}.
 *
 * @param <T> the type of the data returned in the HTTP response body
 */
public class HttpResult<T> {

    private final int status;
    private final T data;

    public HttpResult(int status, T data) {
        this.status = status;
        this.data = data;
    }

    public int getStatus() {
        return status;
    }

    public Optional<T> getData() {
        return Optional.ofNullable(data);
    }
}

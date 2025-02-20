/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.http;

import org.apache.hc.core5.http.ClassicHttpResponse;

import java.io.IOException;

/**
 * The ResponseHandler interface is a functional interface that provides a method
 * to handle HTTP responses and map them to a specified type.
 *
 * @param <T> the type of the response object
 */
public interface ResponseHandler<T> {

    /**
     * Handles the given HTTP response and creates a {@link HttpResult} containing the status code and the mapped
     * result object of type {@link T}.
     *
     * @param response The HTTP response to be handled.
     * @return An HttpResult containing the status code of the response and the mapped response data.
     * @throws IOException If an I/O error occurs during the handling of the response.
     */
    HttpResult<T> handle(ClassicHttpResponse response) throws IOException;

}

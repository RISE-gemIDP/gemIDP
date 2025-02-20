/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import org.springframework.http.HttpStatus;

import java.util.Map;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

public class ExtAuthCodeResponse {

    private final HttpStatus status;
    private final Map<String, String> data;

    ExtAuthCodeResponse(int status, Map<String, String> data) {
        this.status = HttpStatus.resolve(status);
        this.data = requireNonNull(data);
    }

    public boolean hasError() {
        return nonNull(status) && status.isError();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getData() {
        return data;
    }

    public String idToken() {
        return data.get("id_token");
    }
}

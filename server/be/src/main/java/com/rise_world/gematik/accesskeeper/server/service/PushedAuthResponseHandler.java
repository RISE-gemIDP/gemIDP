/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rise_world.gematik.accesskeeper.server.dto.PARResponse;
import com.rise_world.gematik.accesskeeper.server.http.HttpResult;
import com.rise_world.gematik.accesskeeper.server.http.ResponseHandler;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static java.util.Objects.isNull;

@Service
public class PushedAuthResponseHandler implements ResponseHandler<PARResponse> {

    private final ObjectMapper objectMapper;

    public PushedAuthResponseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResult<PARResponse> handle(ClassicHttpResponse response) throws IOException {
        if (isNull(response.getEntity())) {
            return new HttpResult<>(response.getCode(), null);
        }
        return new HttpResult<>(response.getCode(), objectMapper.readValue(response.getEntity().getContent(), PARResponse.class));
    }
}

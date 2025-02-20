/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rise_world.gematik.accesskeeper.server.http.HttpResult;
import com.rise_world.gematik.accesskeeper.server.http.ResponseHandler;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.helpers.IOUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

import static java.util.Objects.isNull;

@Service
public class ExtAuthCodeResponseHandler implements ResponseHandler<Map<String, String>> {

    private final ObjectMapper objectMapper;

    public ExtAuthCodeResponseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResult<Map<String, String>> handle(ClassicHttpResponse response) throws IOException {
        if (isNull(response.getEntity())) {
            return new HttpResult<>(response.getCode(), null);
        }

        if (response.getCode() != 200) {
            return handleError(response);
        }

        return new HttpResult<>(response.getCode(), parseToMap(response));
    }

    private HttpResult<Map<String, String>> handleError(ClassicHttpResponse response) throws IOException {
        if (!isJson(response)) {
            return new HttpResult<>(response.getCode(), Map.of("error", IOUtils.readStringFromStream(response.getEntity().getContent())));
        }

        return new HttpResult<>(response.getCode(), parseToMap(response));
    }

    private Map<String, String> parseToMap(ClassicHttpResponse response) throws IOException {
        return objectMapper.readValue(response.getEntity().getContent(), new TypeReference<>() {
        });
    }

    private static boolean isJson(ClassicHttpResponse response) {
        return MediaType.APPLICATION_JSON.equals(response.getEntity().getContentType());
    }
}

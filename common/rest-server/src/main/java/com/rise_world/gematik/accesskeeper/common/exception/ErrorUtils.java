/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.exception;

import com.rise_world.gematik.idp.server.api.ErrorResponse;

import java.time.Clock;

public class ErrorUtils {

    private ErrorUtils() {
        // avoid instantiation
    }

    public static ErrorResponse toErrorResponse(Clock clock, ErrorMessage errorMessage, String requestId) {
        ErrorResponse response = new ErrorResponse();
        if (errorMessage.getoAuth2Error() != null) {
            response.setError(errorMessage.getoAuth2Error().getValue());
        }
        response.setGematikCode(String.valueOf(errorMessage.getGematikCode()));
        response.setGematikTimestamp(clock.instant().getEpochSecond());
        response.setGematikUuid(requestId);
        response.setGematikErrorText(errorMessage.getText());

        return response;
    }
}


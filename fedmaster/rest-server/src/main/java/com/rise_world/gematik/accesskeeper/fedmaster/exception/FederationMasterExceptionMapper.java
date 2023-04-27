/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.exception;

import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;
import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import com.rise_world.gematik.idp.server.api.FederationErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.time.Clock;

@Provider
@Component
public class FederationMasterExceptionMapper implements ExceptionMapper<FederationMasterException> {

    private static final Logger LOG = LoggerFactory.getLogger(FederationMasterExceptionMapper.class);

    private final Clock clock;

    public FederationMasterExceptionMapper(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Response toResponse(FederationMasterException e) {
        final ErrorMessage errorMessage = e.getErrorMessage();
        final String requestId = MDC.get(LogTool.MDC_REQ_ID);
        LOG.info("FederationMaster Exception was thrown", e);

        return Response.status(errorMessage.getHttpError())
            .type(MediaType.APPLICATION_JSON)
            .entity(toErrorResponse(errorMessage, requestId, e.getOperation().name()))
            .build();
    }

    private FederationErrorResponse toErrorResponse(ErrorMessage errorMessage, String requestId, String operation) {
        FederationErrorResponse response = new FederationErrorResponse();

        response.setOperation(operation);
        if (errorMessage.getoAuth2Error() != null) {
            response.setError(errorMessage.getoAuth2Error().getValue());
        }
        response.setErrorDescription(errorMessage.getText());
        response.setGematikCode(String.valueOf(errorMessage.getGematikCode()));
        response.setGematikTimestamp(clock.instant().getEpochSecond());
        response.setGematikUuid(requestId);

        return response;
    }
}

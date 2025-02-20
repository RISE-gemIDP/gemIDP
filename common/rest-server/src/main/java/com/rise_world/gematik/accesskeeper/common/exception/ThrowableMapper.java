/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.exception;

import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Clock;

/**
 * A 'catch-all'-ExceptionMapper, that avoids to expose internal state in case of an unexpected error
 */
@Provider
@Component
public class ThrowableMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = LoggerFactory.getLogger(ThrowableMapper.class);

    private Clock clock;

    @Autowired
    public ThrowableMapper(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Response toResponse(Throwable throwable) {
        // don't transform client errors to internal errors
        if (throwable instanceof ClientErrorException c) {
            return Response.status(c.getResponse().getStatus()).build();
        }

        LOG.error("An unexpected error occured", throwable);
        final String requestId = MDC.get(LogTool.MDC_REQ_ID);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .type(MediaType.APPLICATION_JSON)
            .entity(ErrorUtils.toErrorResponse(clock, ErrorCodes.SERVER_ERROR, requestId))
            // @AFO: A_20246_1 - mappen von allen internen Fehlern des IDP
            .header(PerfLogConstants.HEADER_PERF_DIENST_OPERATION, PerfLogConstants.IDP_PERF_FAILED)
            .header(PerfLogConstants.HEADER_PERF_ERRORCODE, PerfLogConstants.IDP_PERF_INTERNAL_IDP_ERROR)
            .header(PerfLogConstants.HEADER_PERF_GEMATIKCODE, ErrorCodes.SERVER_ERROR.getGematikCode())
            .build();
    }
}

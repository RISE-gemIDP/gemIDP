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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.time.Clock;

@Provider
@Component
public class AccessKeeperExceptionMapper implements ExceptionMapper<AccessKeeperException> {

    private static final Logger LOG = LoggerFactory.getLogger(AccessKeeperExceptionMapper.class);

    private Clock clock;

    @Autowired
    public AccessKeeperExceptionMapper(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Response toResponse(AccessKeeperException e) {
        final ErrorMessage errorMessage = e.getErrorMessage();
        final String requestId = MDC.get(LogTool.MDC_REQ_ID);
        LOG.info("AccessKeeper Exception was thrown", e);

        Response.ResponseBuilder response = Response.status(errorMessage.getHttpError())
            .type(MediaType.APPLICATION_JSON)
            .entity(ErrorUtils.toErrorResponse(clock, errorMessage, requestId));

        return setPerfHeaders(response, e)
            .build();
    }

    private Response.ResponseBuilder setPerfHeaders(Response.ResponseBuilder builder, AccessKeeperException e) {
        String perfOperation = null;
        Integer perfErrorCode = null;

        if (e.getErrorMessage() == ErrorCodes.AUTH_OCSP_ERROR_NO_RESPONSE) {
            perfOperation = PerfLogConstants.IDP_PERF_OCSP_OPERATION;
            perfErrorCode = PerfLogConstants.IDP_PERF_ERROR_NO_RESPONSE;
        }
        if (e.getCause() instanceof CertificateServiceException) {
            perfOperation = PerfLogConstants.IDP_PERF_OCSP_OPERATION;
            int certServiceCode = ((CertificateServiceException) e.getCause()).getCode();

            if (certServiceCode == PerfLogConstants.CS_OCSP_NOT_AVAILABLE) {
                perfErrorCode = PerfLogConstants.IDP_PERF_ERROR_NO_RESPONSE;
            }
            else if (certServiceCode == PerfLogConstants.CS_OCSP_SIGNATURE_ERROR) {
                perfErrorCode = PerfLogConstants.IDP_PERF_ERROR_WRONG_SIGNATURE;
            }
            else if (certServiceCode == PerfLogConstants.CS_OCSP_STATUS_ERROR_MALFORMED_REQUEST) {
                perfOperation = PerfLogConstants.IDP_PERF_OCSP_OPERATION_FAILED;
                perfErrorCode = PerfLogConstants.IDP_PERF_ERROR_WRONG_DATA;
            }
            else if (certServiceCode == PerfLogConstants.CS_CERTHASH_EXTENSION_MISSING ||
                certServiceCode == PerfLogConstants.CS_OCSP_CHECK_REVOCATION_ERROR ||
                certServiceCode == PerfLogConstants.CS_CERTHASH_MISMATCH) {
                // CERTHASH_EXTENSION_MISSING
                perfErrorCode = PerfLogConstants.IDP_PERF_ERROR_INVALID_RESPONSE;
            }
            else if (certServiceCode == PerfLogConstants.CS_OCSP_NO_CERTIFICATE) {
                perfErrorCode = PerfLogConstants.IDP_PERF_CERT_MISSING;
            }
        }
        if (perfOperation != null && perfErrorCode != null) {
            return builder
                .header(PerfLogConstants.HEADER_PERF_DIENST_OPERATION, perfOperation)
                .header(PerfLogConstants.HEADER_PERF_ERRORCODE, perfErrorCode);
        }
        return builder;
    }
}


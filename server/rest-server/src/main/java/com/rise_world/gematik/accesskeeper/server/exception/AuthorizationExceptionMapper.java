/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.exception;

import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorUtils;
import com.rise_world.gematik.accesskeeper.common.exception.OAuth2Error;
import com.rise_world.gematik.accesskeeper.common.util.UrlBuilder;
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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.time.Clock;

@Provider
@Component
public class AuthorizationExceptionMapper implements ExceptionMapper<AuthorizationException> {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationExceptionMapper.class);

    private static final String CHARSET = "UTF-8";

    private Clock clock;

    @Autowired
    public AuthorizationExceptionMapper(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Response toResponse(AuthorizationException e) {
        final ErrorMessage errorMessage = e.getErrorMessage();
        final String requestId = MDC.get(LogTool.MDC_REQ_ID);

        if (e.getRedirectUri() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorUtils.toErrorResponse(clock, errorMessage, requestId))
                .build();
        }

        UrlBuilder redirectUri = new UrlBuilder(e.getRedirectUri());

        if (errorMessage.getGematikCode() != ErrorCodes.AUTH_MISSING_STATE_PARAMETER.getGematikCode() &&
            errorMessage.getGematikCode() != ErrorCodes.AUTH_INVALID_STATE_PARAMETER.getGematikCode()) {
            redirectUri.appendState(e.getState()); // @AFO A_20376 - "state"-Parameter wird zur Redirect-URL hinzugefügt
        }

        OAuth2Error oAuth2Error = errorMessage.getoAuth2Error();
        if (errorMessage.getoAuth2Error() == null) {
            oAuth2Error = OAuth2Error.SERVER_ERROR;
        }
        redirectUri.appendParameter("error", oAuth2Error.getValue());

        redirectUri.appendParameter("gematik_code", errorMessage.getGematikCode());
        redirectUri.appendParameter("gematik_timestamp", clock.instant().getEpochSecond());
        if (requestId != null) {
            redirectUri.appendParameter("gematik_uuid", requestId);
        }
        try {
            String encodedErrorMsg = URLEncoder.encode(errorMessage.getText(), CHARSET);
            redirectUri.appendParameter("gematik_error_text", encodedErrorMsg);
        }
        catch (UnsupportedEncodingException ex) {
            LOG.error("Failed to convert error description", ex);
        }

        return Response.status(Response.Status.FOUND)
            .location(URI.create(redirectUri.toString()))
            .build();
    }
}

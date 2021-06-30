/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairing.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorUtils;
import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import com.rise_world.gematik.accesskeeper.pairingdienst.dto.AccessTokenDTO;
import com.rise_world.gematik.accesskeeper.pairingdienst.service.AccessTokenParser;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

import static com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes.REG1_CLIENT_ERROR;

/**
 * Filter that performs authorization checks.
 * <p>
 * If a protected path is accessed, this filter expects an encrypted access token as header parameter "AUTHORIZATION", using scheme "Bearer".
 * The filter validates this access token using {@link AccessTokenParser}.
 */
@Order(2)
@Component
public class AuthorizationFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationFilter.class);

    private static final String AUTH_SCHEME = "Bearer";

    private AccessTokenParser accessTokenParser;
    private UnprotectedPathConfig unprotectedPathConfig;
    private ObjectMapper objectMapper;
    private Clock clock;

    @Autowired
    public AuthorizationFilter(AccessTokenParser accessTokenParser, UnprotectedPathConfig unprotectedPathConfig, ObjectMapper objectMapper, Clock clock) {
        this.accessTokenParser = accessTokenParser;
        this.unprotectedPathConfig = unprotectedPathConfig;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String requestURI = request.getRequestURI();

        if (requestURI != null) {
            boolean requestToUnprotectedPath = unprotectedPathConfig.getUnprotectedPaths().stream().anyMatch(requestURI::startsWith);

            if (requestToUnprotectedPath) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if ((authHeader == null) || !authHeader.startsWith(AUTH_SCHEME)) {
            LOG.warn("invalid authorization header");
            returnError(REG1_CLIENT_ERROR, response);
            return;
        }

        try {
            String encryptedAccessToken = authHeader.substring(AUTH_SCHEME.length()).trim();
            // @AFO: A_21442 Authentisierung des Nutzers durch den übermittelten Access Token
            JwtClaims accessToken = accessTokenParser.extractAndValidate(encryptedAccessToken);
            // @AFO: A_21452 Extrahieren der idNummer aus dem AccessToken
            AccessTokenDTO dto = new AccessTokenDTO(accessToken.getStringProperty("idNummer"), accessToken.getListStringProperty("amr"));

            AuthorizationContext.setAccessToken(dto);
            filterChain.doFilter(request, response);
        }
        catch (AccessKeeperException ex) {
            returnError(ex.getErrorMessage(), response);
        }
        catch (Exception ex) {
            LOG.warn("invalid access token", ex);
            returnError(REG1_CLIENT_ERROR, response);
        }
        finally {
            AuthorizationContext.clear();
        }
    }


    private void returnError(ErrorMessage missingAccessToken, HttpServletResponse response) throws IOException {
        String requestId = MDC.get(LogTool.MDC_REQ_ID);

        response.setStatus(missingAccessToken.getHttpError());
        response.setContentType(MediaType.APPLICATION_JSON);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorUtils.toErrorResponse(clock, missingAccessToken, requestId));
        response.getWriter().flush();
    }
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.api.util;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public class ParameterUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ParameterUtils.class);

    private ParameterUtils() {
    }

    /**
     * Validates that no request parameter is included more than once.
     *
     * @param request the HTTP servlet request
     * @param m       the {@link ErrorMessage} that will be used in case of an error
     * @throws AccessKeeperException if a parameter is included more than once
     */
    // @AFO: A_20434 - Parameter d&uuml;rfen nicht doppelt vorkommen
    public static void validateDuplicateParameters(HttpServletRequest request, ErrorMessage m) {
        final Map<String, String[]> parameterMap = request.getParameterMap();
        for (String[] value : parameterMap.values()) {
            if (value != null && value.length > 1) {
                LOG.warn("Client sent duplicate request parameters");
                throw new AccessKeeperException(m);
            }
        }
    }

    /**
     * Validates that 'client_id' and 'redirect_uri' are not included more than once in the request.
     *
     * @param request the HTTP servlet request
     * @throws AccessKeeperException if 'client_id' or 'redirect_uri' are included more than once
     */
    public static void validateDuplicateClientIdRedirectUri(HttpServletRequest request) {
        final String[] clientIds = request.getParameterMap().get("client_id");
        final String[] redirectUris = request.getParameterMap().get("redirect_uri");
        if ((clientIds != null && clientIds.length > 1) || (redirectUris != null && redirectUris.length > 1)) {
            LOG.warn("Client sent duplicate client_id/redirect_uri");
            throw new AccessKeeperException(ErrorCodes.AUTH_DUPLICATE_PARAMETERS);
        }
    }
}

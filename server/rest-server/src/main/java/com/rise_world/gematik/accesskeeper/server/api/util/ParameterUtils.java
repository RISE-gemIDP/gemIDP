/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.api.util;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class ParameterUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ParameterUtils.class);

    private ParameterUtils() {
    }

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
}

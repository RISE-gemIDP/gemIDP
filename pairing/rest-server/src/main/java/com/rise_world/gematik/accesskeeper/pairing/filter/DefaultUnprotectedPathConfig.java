/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairing.filter;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;


@Component
public class DefaultUnprotectedPathConfig implements  UnprotectedPathConfig {

    private static final List<String> UNPROTECTED_PATHS = Arrays.asList("/idpinternal/", "/internal/actuator");

    public List<String> getUnprotectedPaths() {
        return UNPROTECTED_PATHS;
    }
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.util;

import java.net.URI;
import java.net.URISyntaxException;

import static java.util.Objects.nonNull;

/**
 * Utility class to enforce valid https uris
 */
public class HttpsEnforcer {

    private HttpsEnforcer() { /* utility class */ }

    /**
     * {@code isHttps} checks, if the given {@code uri} is valid and uses {@code https} scheme.
     *
     * @param uri uri to validate
     * @return {@code true} if the {@code uri} is valid and uses {@code https} scheme, false otherwise
     */
    public static boolean isHttps(String uri) {
        try {
            return nonNull(uri) && "https".equals(new URI(uri).getScheme());
        }
        catch (URISyntaxException e) {
            return false;
        }
    }

}

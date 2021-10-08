/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.util;

public class LoopbackUtils {

    public static final String LOOPBACK_PREFIX_IPV4 = "http://127.0.0.1";
    public static final String LOOPBACK_PREFIX_IPV6 = "http://[::1]";

    private LoopbackUtils() {
    }

    // @AFO: A_20434 - wenn die konfigurierte URI eine Loopback Adresse ist, dann darf sich der Port unterscheiden
    public static boolean matchesLoopback(String configuredUri, String redirectUri) {
        if (isLoopbackUri(LOOPBACK_PREFIX_IPV4, redirectUri) && isLoopbackUri(LOOPBACK_PREFIX_IPV4, configuredUri) &&
            normalizeLoopbackUri(LOOPBACK_PREFIX_IPV4, redirectUri).equals(normalizeLoopbackUri(LOOPBACK_PREFIX_IPV4, configuredUri))) {
            return true;
        }
        else {
            return isLoopbackUri(LOOPBACK_PREFIX_IPV6, redirectUri) && isLoopbackUri(LOOPBACK_PREFIX_IPV6, configuredUri) &&
                normalizeLoopbackUri(LOOPBACK_PREFIX_IPV6, redirectUri).equals(normalizeLoopbackUri(LOOPBACK_PREFIX_IPV6, configuredUri));
        }
    }

    public static boolean isLoopbackUri(String loopbackPrefix, String redirectUri) {
        if (!redirectUri.startsWith(loopbackPrefix)) {
            return false;
        }
        else {
            final int prefixLength = loopbackPrefix.length();
            if (prefixLength == redirectUri.length()) {
                return true;
            }
            else {
                final char nextChar = redirectUri.charAt(prefixLength);
                return nextChar == ':' || nextChar == '/' || nextChar == '?';
            }
        }
    }

    public static String normalizeLoopbackUri(String loopbackPrefix, String redirectUri) {
        final int prefixLength = loopbackPrefix.length();

        // if there is no port definition return uri without changes
        if (redirectUri.length() == prefixLength || redirectUri.charAt(prefixLength) != ':') {
            return redirectUri;
        }
        // remove port declaration
        else {
            int idx = prefixLength + 1;

            for (; idx < redirectUri.length(); idx++) {
                if (redirectUri.charAt(idx) == '/' || redirectUri.charAt(idx) == '?') {
                    break;
                }
            }

            if (!isValidPort(redirectUri, prefixLength + 1, idx)) {
                return "";
            }

            return loopbackPrefix + redirectUri.substring(idx);
        }
    }

    private static boolean isValidPort(String redirectUri, int startIdx, int endIdx) {
        if (startIdx == endIdx) {
            return true;
        }

        if (endIdx - startIdx > 5) {
            return false;
        }
        try {
            int port = Integer.parseInt(redirectUri.substring(startIdx, endIdx));
            if (port < 1 || port > 65535) {
                return false;
            }
        }
        catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}

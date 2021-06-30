/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class LangUtils {

    private static final Pattern BASE64URL = Pattern.compile("^(?:[A-Za-z0-9_\\-]{4})*(?:[A-Za-z0-9_\\-]{2}==|[A-Za-z0-9_\\-]{3}=|[A-Za-z0-9_\\-]{4})$");
    private static final Pattern BASE64URL_WITHOUT_PADDING = Pattern.compile("^(?:[A-Za-z0-9_\\-]{4})*(?:[A-Za-z0-9_\\-]{2,4})$");

    private LangUtils() {
        // avoid instantiation
    }

    /**
     * Checks if a String is null, empty or contains only whitespace characters (incl. non-breaking spaces)
     * <p>
     * see https://issues.apache.org/jira/browse/LANG-1148
     * @param s the String to check, can be null
     * @return {@code true} if the String is null, empty or whitespace only
     */
    public static boolean isBlankOrEmpty(String s) {
        return StringUtils.trimToEmpty(s).chars().allMatch(LangUtils::isWhitespace);
    }

    public static boolean isWhitespace(int ch) {
        return Character.isSpaceChar(ch) ||
            Character.isWhitespace(ch) ||
            ch == '\u0085' ||
            ch == '\u200b' ||
            ch == '\u200c' ||
            ch == '\u200d' ||
            ch == '\u2060' ||
            ch == '\u0fef';
    }

    public static boolean isBase64Url(String encoded, boolean withPadding) {
        Pattern pattern = withPadding ? BASE64URL : BASE64URL_WITHOUT_PADDING;
        return pattern.matcher(encoded).matches();
    }

}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.util;

import com.rise_world.gematik.accesskeeper.common.util.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class to create and verify code verifiers
 */
public class PkceUtils {

    private static char[] codeVerifierChars;
    private static SecureRandom secureRandom;

    static {
        // initialize code verifier characters defined in RFC7636
        // code_verifier = high-entropy cryptographic random STRING using the
        //   unreserved characters [A-Z] / [a-z] / [0-9] / "-" / "." / "_" / "~"
        //   from Section 2.3 of [RFC3986]

        StringBuilder chars = new StringBuilder();
        for (char c = 'A'; c <= 'Z'; c++) {
            chars.append(c);
        }
        for (char c = 'a'; c <= 'z'; c++) {
            chars.append(c);
        }
        for (char c = '0'; c <= '9'; c++) {
            chars.append(c);
        }
        chars.append('-').append('.').append('_').append('~');
        codeVerifierChars = chars.toString().toCharArray();

        secureRandom = new SecureRandom();
    }

    private PkceUtils() {
    }

    public static String createCodeVerifier() {
        return RandomStringUtils.random(64, 0, 0, true, true, codeVerifierChars, secureRandom);
    }

    public static String createCodeChallenge(String codeVerifier) {
        byte[] hashBytes = DigestUtils.sha256(codeVerifier.getBytes());
        // base64 url encoding without padding as defined in RFC7636 Appendix A
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
    }
}

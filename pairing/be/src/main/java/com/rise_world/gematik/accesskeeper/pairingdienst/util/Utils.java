/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.util;

import java.security.MessageDigest;
import java.util.Base64;

public class Utils {

    public static final Base64.Decoder BASE64URL_DECODER = Base64.getUrlDecoder();
    public static final Base64.Encoder BASE64URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private Utils() {
        // avoid instantiation
    }

    /**
     * Compares two byte arrays for equality
     * <p>
     * Note: If the digests are the same length, all bytes are examined to determine equality.
     *
     * @param a byte array
     * @param b byte array
     * @return true if and only if the byte arrays are equal
     * @see MessageDigest#isEqual(byte[], byte[])
     */
    public static boolean timeConstantNotEquals(byte[] a, byte[] b) {
        return !MessageDigest.isEqual(a, b);
    }
}

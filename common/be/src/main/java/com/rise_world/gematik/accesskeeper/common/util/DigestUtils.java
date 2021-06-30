/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.util;

import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestUtils {

    private static final String SHA_256 = "SHA-256";

    private DigestUtils() {
    }

    public static String sha256Hex(String message, Charset charset) {
        return Hex.toHexString(sha256(message.getBytes(charset)));
    }

    public static byte[] sha256(byte[] message) {
        return getDigest(SHA_256).digest(message);
    }

    private static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        }
        catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

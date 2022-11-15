/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.util;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;

import java.security.SecureRandom;
import java.util.UUID;

public class RandomUtils {

    private static final SecureRandom NUMBER_GENERATOR = new SecureRandom();

    private RandomUtils() {
        // avoid instantiation
    }

    /**
     * Creates a random Java UUID
     * @return the random UUID
     * @AFO GS-A_4367 - zuf&auml;llige UUID wird generiert (zB f&uuml;r JWTs)
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Creates a random Java UUID without hyphens.
     * @return the generated random UUID without hyphens
     */
    public static String randomShortUUID() {
        return StringUtils.remove(randomUUID(), '-');
    }

    /**
     * Creates a random hex string.
     * @param length length of bytes to be used for the random hex string
     * @return hexadecimal string representation of the random bytes generated
     */
    public static String randomHex(int length) {
        byte[] bytes = new byte[length];
        NUMBER_GENERATOR.nextBytes(bytes);
        return Hex.toHexString(bytes);
    }

}

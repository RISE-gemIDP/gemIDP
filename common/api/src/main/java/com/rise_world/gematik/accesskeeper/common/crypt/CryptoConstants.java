/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.crypt;

public class CryptoConstants {

    public static final String BOUNCY_CASTLE = "BC";
    public static final String BRAINPOOL_P256R1_CURVE_BC = "brainpoolP256r1";
    public static final String SIG_ALG_BRAINPOOL_P256_R1 = "BP256R1";
    public static final String JWE_BRAINPOOL_CURVE = "BP-256";
    public static final String CURVE_SEC_P256_R1 = "secp256r1";

    private CryptoConstants() {
        // avoid instantiation
    }
}

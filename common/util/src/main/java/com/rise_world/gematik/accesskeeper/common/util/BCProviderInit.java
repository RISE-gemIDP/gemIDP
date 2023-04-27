/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

import java.security.Security;

public class BCProviderInit {

    private static final String[] SUPPORTED_CURVES = {
        "brainpoolP256r1",
        "brainpoolP384r1",
        "brainpoolP512r1",
        "secp256r1",
        "secp384r1"
    };

    public static void init() {
        Security.insertProviderAt(new BouncyCastleJsseProvider(), 1);
        Security.insertProviderAt(new BouncyCastleProvider(), 2);

        System.setProperty("jdk.tls.namedGroups", String.join(",", SUPPORTED_CURVES));
    }
}

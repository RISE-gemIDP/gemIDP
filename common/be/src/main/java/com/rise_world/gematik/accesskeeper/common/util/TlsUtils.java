/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.util;

import org.apache.cxf.configuration.jsse.TLSClientParameters;

import java.util.Arrays;

public class TlsUtils {

    private static final String[] SUPPORTED_CIPHER_SUITES = {
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
    };

    private TlsUtils() {
        // avoid instantiation
    }

    public static TLSClientParameters createTLSClientParameters() {
        TLSClientParameters tlsClientParameters = new TLSClientParameters();
        tlsClientParameters.setSecureSocketProtocol("TLSv1.2");
        tlsClientParameters.setCipherSuites(Arrays.asList(SUPPORTED_CIPHER_SUITES));
        return tlsClientParameters;
    }
}

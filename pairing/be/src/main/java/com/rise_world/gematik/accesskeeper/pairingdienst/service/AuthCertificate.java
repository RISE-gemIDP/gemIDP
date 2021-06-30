/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.service;

import java.security.cert.X509Certificate;
import java.util.function.Function;
import static com.rise_world.gematik.accesskeeper.pairingdienst.util.Utils.BASE64URL_DECODER;

/**
 * An authentication certificate.
 */
public class AuthCertificate {

    private final String authCertificateAsBase64urlString;

    private final Function<byte[], X509Certificate> certificateParser;

    private byte[] authCertificateAsBytes;
    private X509Certificate authCertificateAsX509;

    public AuthCertificate(String authCertificateAsBase64urlString, Function<byte[], X509Certificate> certificateParser) {
        this.authCertificateAsBase64urlString = authCertificateAsBase64urlString;
        this.certificateParser = certificateParser;

    }

    /**
     * Returns the certificate as bytes.
     *
     * @return certificate as bytes
     */
    public byte[] asBytes() {
        if (authCertificateAsBytes == null) {
            authCertificateAsBytes = BASE64URL_DECODER.decode(authCertificateAsBase64urlString);
        }

        return authCertificateAsBytes;
    }

    /**
     * Returns the certificate as {@link X509Certificate}.
     *
     * @return certificate as {@link X509Certificate}
     */
    public X509Certificate asX509Certificate() {
        if (authCertificateAsX509 == null) {
            authCertificateAsX509 = certificateParser.apply(asBytes());
        }

        return authCertificateAsX509;
    }
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

public class CertificateTransparencyProviderException extends RuntimeException {

    public CertificateTransparencyProviderException(String message) {
        super(message);
    }

    public CertificateTransparencyProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}

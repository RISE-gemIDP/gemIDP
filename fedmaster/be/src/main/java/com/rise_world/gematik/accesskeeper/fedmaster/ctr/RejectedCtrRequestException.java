/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

/**
 * Exception indicating rejected data provided in request (i.e. invalid TLD)
 */
public class RejectedCtrRequestException extends CertificateTransparencyProviderException {

    public RejectedCtrRequestException(String message) {
        super(message);
    }

    public RejectedCtrRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}

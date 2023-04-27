/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

/**
 * Exception indicating that the ctr provider declined the request (i.e. unknown endpoint, too many requests or unauthorized user)
 */
public class CtrServiceException extends CertificateTransparencyProviderException {

    public CtrServiceException(String message) {
        super(message);
    }

    public CtrServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

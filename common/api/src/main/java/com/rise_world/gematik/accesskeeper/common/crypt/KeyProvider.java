/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.crypt;

import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;

/**
 * Interface for providing certificates and public keys
 */
public interface KeyProvider {

    /**
     * Provides the appropriate certificate
     *
     * @param kid identifier for the certificate to be returned
     * @return the appropriate Certificate for the requested identifier, if none can be found an
     *         {@link IllegalArgumentException} should be thrown
     */
    X509Certificate getCertificate(String kid);

    /**
     * Provides the appropriate elliptic curve public key
     *
     * @param kid identifier for the key to be returned
     * @return the appropriate ECPublicKey for the requested identifier, if none can be found an
     *         {@link IllegalArgumentException} should be thrown
     */
    ECPublicKey getKey(String kid);
}

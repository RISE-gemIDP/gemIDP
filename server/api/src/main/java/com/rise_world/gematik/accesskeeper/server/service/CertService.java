/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;

/**
 * Interface for retrieving the different public keys of the provided endpoints represented as JWK
 */
public interface CertService {

    /**
     * Retrieves signature certificate of the discovery endpoint
     *
     * @return Json Web Key from puk_disc_sig
     */
    JsonWebKey getDiscoveryCert();

    /**
     * Retrieves signature certificate of the authorization/token endpoint
     *
     * @return Json Web Key from puk_idp_sig
     */
    JsonWebKey getSignatureCert();

    /**
     * Retrieves the public key of the encryption key pair
     * @return Json Web Key from puk_idp_enc
     */
    JsonWebKey getEncryptionKey();
}

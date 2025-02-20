/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.service;

import org.apache.cxf.rs.security.jose.common.JoseConstants;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Token endpoint definition for signed_jwks_uri
 */
public interface SignedJwksEndpoint {

    @GET
    @Produces({JoseConstants.MEDIA_TYPE_JOSE, MediaType.TEXT_PLAIN, "application/jwk-set+json"})
    String getSignedJwks();
}

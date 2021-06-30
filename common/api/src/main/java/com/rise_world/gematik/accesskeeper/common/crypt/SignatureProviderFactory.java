/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.crypt;

import com.rise_world.gematik.accesskeeper.common.dto.Endpoint;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;

/**
 * Factory that creates JwsSignatureProvider for an endpoint
 */
public interface SignatureProviderFactory {

    /**
     * Creates a JwsSignatureProvider for an endpoint
     *
     * @param endpoint for which a JwsSignatureProvider should be created
     * @return a JwsSignatureProvider
     */
    JwsSignatureProvider createSignatureProvider(Endpoint endpoint);
}

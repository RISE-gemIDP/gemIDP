/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.token.extraction.parser;

import com.rise_world.gematik.accesskeeper.common.crypt.CryptoConstants;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsException;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class IdpJwsJwtCompactConsumer extends JwsJwtCompactConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(IdpJwsJwtCompactConsumer.class);

    private final String encodedJws;

    public IdpJwsJwtCompactConsumer(String encodedJws) {
        super(encodedJws);
        this.encodedJws = encodedJws;
    }

    @Override
    public boolean verifySignatureWith(JwsSignatureVerifier validator) {
        try {
            JwsHeaders headerCopy = new JwsHeaders(new HashMap<>(getJwsHeaders().asMap())); // copy headers to preserve the original values

            // cxf doesn't know non-standard algorithm BP256R1, therefore we have to set the signatureAlgorithm to a known value
            if (CryptoConstants.SIG_ALG_BRAINPOOL_P256_R1.equals(headerCopy.getAlgorithm())) {
                headerCopy.setSignatureAlgorithm(SignatureAlgorithm.ES256);
            }
            if (validator.verify(headerCopy, getUnsignedEncodedSequence(), getDecodedSignature())) {
                return true;
            }
        }
        catch (JwsException ex) {
            LOG.error("Failed to validate signature", ex); // cxf doesn't log jws exceptions,
        }

        LOG.warn("Invalid Signature");
        return false;
    }

    public String getEncodedJws() {
        return encodedJws;
    }
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.token.creation;

import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;

import java.util.Objects;

/**
 * A custom subclass of {@link JwsHeaders} that allows to set signatureAlgorithm and algorithm independently.
 * <p>
 * This is necessary because CXFs {@link SignatureAlgorithm} enumeration doesn't know the non-standard algorithm BP256R1.
 */
public class IdpJwsHeaders extends JwsHeaders {

    private SignatureAlgorithm algorithm;

    public IdpJwsHeaders(SignatureAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public SignatureAlgorithm getSignatureAlgorithm() {
        return algorithm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        IdpJwsHeaders that = (IdpJwsHeaders) o;
        return algorithm == that.algorithm;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), algorithm);
    }
}

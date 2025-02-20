/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;

import java.util.Set;

import static java.util.Objects.nonNull;

/**
 * Utility class to check supported signature algorithms
 */
class SupportedSignatureAlgorithms {

    private static final Set<SignatureAlgorithm> SUPPORTED_ALGORITHMS = Set.of(SignatureAlgorithm.ES256, SignatureAlgorithm.ES384);

    private SupportedSignatureAlgorithms() { /* utility class */ }

    static boolean isSupported(SignatureAlgorithm signatureAlgorithm) {
        return nonNull(signatureAlgorithm) && SUPPORTED_ALGORITHMS.contains(signatureAlgorithm);
    }
}

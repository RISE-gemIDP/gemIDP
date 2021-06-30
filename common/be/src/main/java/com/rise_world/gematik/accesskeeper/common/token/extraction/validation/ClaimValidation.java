/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.token.extraction.validation;

/**
 * Defines a validation rule for claims
 */
public interface ClaimValidation<T> {

    /**
     * Simple implementation which is valid regardless of the supplied parameter
     *
     * @param <T> type of the validation
     * @return the 'always-valid' validation
     */
    static <T> ClaimValidation<T> alwaysValid() {
        return any -> {
        };
    }

    /**
     * Checks if claims fulfil a validation rule. If check fails an AccessKeeperException will be thrown.
     *
     * @param claims to be checked
     */
    void validate(T claims);
}

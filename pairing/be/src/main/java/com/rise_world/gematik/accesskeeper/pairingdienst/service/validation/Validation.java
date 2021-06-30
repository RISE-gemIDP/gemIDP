/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.service.validation;

/**
 * Defines the logic to validate a given object type {@code T}.
 *
 * @param <T> the target type supported by an implementation
 */
public interface Validation<T> {

    /**
     * Implements the validation logic
     *
     * @param value object to validate
     * @return {@code true} if {@code value} passes the validation rule
     */
    boolean isValid(T value);

}

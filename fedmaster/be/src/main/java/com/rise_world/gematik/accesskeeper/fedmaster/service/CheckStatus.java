/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

public enum CheckStatus {
    OK,
    NOK,
    NO_CHECK;

    /**
     * {@code accumulate} returns {@link #OK} if either of the provided statuses is {@link #OK},
     * otherwise, it returns {@link #NOK}.
     *
     * @param status1 the first {@link CheckStatus} value
     * @param status2 the second {@link CheckStatus} value
     * @return {@link #OK} if either {@code status1} or {@code status2} is {@link #OK},
     * otherwise {@link #NOK}
     */
    public static CheckStatus accumulate(CheckStatus status1, CheckStatus status2) {
        if (status1 == OK || status2 == OK) {
            return OK;
        }

        return NOK;
    }

}

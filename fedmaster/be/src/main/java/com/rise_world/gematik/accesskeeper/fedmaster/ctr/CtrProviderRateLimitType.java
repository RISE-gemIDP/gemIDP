/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

public enum CtrProviderRateLimitType {

    /**
     * {@code WAIT_AND_CONTINUE} waits for the time window to elapse and then continues executing further requests
     */
    WAIT_AND_CONTINUE,

    /**
     * {@code TERMINATE} stops the execution, and no further requests will be executed
     */
    TERMINATE
}

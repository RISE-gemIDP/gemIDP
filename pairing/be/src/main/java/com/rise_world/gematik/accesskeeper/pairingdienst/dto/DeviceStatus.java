/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.dto;

/**
 * Status of a device.
 * <p>
 * The device status describes if a device is permitted for registration and
 * authorization.
 */
public enum DeviceStatus {

    /**
     * Device is on block list, hence not permitted for registration and authorization.
     */
    BLOCK,

    /**
     * Device is on allow list, hence permitted for registration and authorization without restrictions.
     */
    ALLOW,

    /**
     * Device is neither on block nor an allow list, hence permitted for registration and authorization only with
     * restrictions.
     */
    UNKNOWN
}

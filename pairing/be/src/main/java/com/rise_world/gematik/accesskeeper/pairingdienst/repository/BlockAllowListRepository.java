/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.repository;

import com.rise_world.gematik.accesskeeper.pairingdienst.dto.DeviceStatus;
import com.rise_world.gematik.accesskeeper.pairingdienst.dto.DeviceTypeDTO;

/**
 * Repository for querying the block/allow list.
 */
public interface BlockAllowListRepository {

    /**
     * Gets the status of the device with the given device type.
     *
     * @param deviceType device type
     * @return {@code DeviceStatus#ALLOW} if the device has status "allow"; {@code DeviceStatus#BLOCK} if the
     *         device has status "block"; otherwise {@code DeviceStatus#UNKNOWN}
     */
    DeviceStatus fetchDeviceStatus(DeviceTypeDTO deviceType);
}

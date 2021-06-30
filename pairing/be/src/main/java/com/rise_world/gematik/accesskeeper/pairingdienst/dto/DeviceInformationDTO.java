/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rise_world.gematik.accesskeeper.pairingdienst.service.validation.Validation;
import com.rise_world.gematik.accesskeeper.pairingdienst.service.validation.Validations;

/**
 * Provides device type and the device name provided by the user.
 */
public class DeviceInformationDTO {

    private static final Validation<String> EXPECTED_VERSION = Validations.expect("1.0");
    private static final Validation<String> MAX_FIELD_LENGTH = Validations.maxLength(300);

    @JsonProperty("device_information_data_version")
    private String deviceInformationDataVersion;

    // contains device name chosen by user (for instance "My iPhone")
    private String name;

    @JsonProperty("device_type")
    private DeviceTypeDTO deviceType;

    @JsonIgnore
    public boolean isValid() {
        return EXPECTED_VERSION.isValid(deviceInformationDataVersion) &&
            MAX_FIELD_LENGTH.isValid(name) &&
            (deviceType != null && deviceType.isValid());
    }

    public String getDeviceInformationDataVersion() {
        return deviceInformationDataVersion;
    }

    public void setDeviceInformationDataVersion(String deviceInformationDataVersion) {
        this.deviceInformationDataVersion = deviceInformationDataVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DeviceTypeDTO getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceTypeDTO deviceType) {
        this.deviceType = deviceType;
    }
}

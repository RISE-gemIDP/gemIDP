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
 * Describes a device type.
 */
public class DeviceTypeDTO {

    private static final Validation<String> EXPECTED_VERSION = Validations.expect("1.0");
    private static final Validation<String> MAX_FIELD_LENGTH = Validations.maxLength(300);

    @JsonProperty("device_type_data_version")
    private String deviceTypeDataVersion;

    private String manufacturer;
    private String product;
    private String model;
    private String os;

    @JsonProperty("os_version")
    private String osVersion;

    @JsonIgnore
    public boolean isValid() {
        return EXPECTED_VERSION.isValid(deviceTypeDataVersion) &&
                MAX_FIELD_LENGTH.isValid(manufacturer) &&
                MAX_FIELD_LENGTH.isValid(product) &&
                MAX_FIELD_LENGTH.isValid(model) &&
                MAX_FIELD_LENGTH.isValid(os) &&
                MAX_FIELD_LENGTH.isValid(osVersion);
    }

    public String getDeviceTypeDataVersion() {
        return deviceTypeDataVersion;
    }

    public void setDeviceTypeDataVersion(String deviceTypeDataVersion) {
        this.deviceTypeDataVersion = deviceTypeDataVersion;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }
}

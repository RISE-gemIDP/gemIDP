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
 * Contains all data needed to register a new pairing.
 */
public class RegistrationDataDTO {

    private static final Validation<String> EXPECTED_VERSION = Validations.expect("1.0");

    @JsonProperty("registration_data_version")
    private String registrationDataVersion;

    @JsonProperty("signed_pairing_data")
    private String signedPairingData;

    @JsonProperty("auth_cert")
    private String authCertificate;

    @JsonProperty("device_information")
    private DeviceInformationDTO deviceInformation;

    @JsonIgnore
    public boolean isValid() {
        return EXPECTED_VERSION.isValid(registrationDataVersion) &&
            (signedPairingData != null) &&
            (authCertificate != null) &&
            (deviceInformation != null && deviceInformation.isValid());
    }

    public String getRegistrationDataVersion() {
        return registrationDataVersion;
    }

    public void setRegistrationDataVersion(String registrationDataVersion) {
        this.registrationDataVersion = registrationDataVersion;
    }

    public String getSignedPairingData() {
        return signedPairingData;
    }

    public void setSignedPairingData(String signedPairingData) {
        this.signedPairingData = signedPairingData;
    }

    public String getAuthCertificate() {
        return authCertificate;
    }

    public void setAuthCertificate(String authCertificate) {
        this.authCertificate = authCertificate;
    }

    public DeviceInformationDTO getDeviceInformation() {
        return deviceInformation;
    }

    public void setDeviceInformation(DeviceInformationDTO deviceInformation) {
        this.deviceInformation = deviceInformation;
    }
}

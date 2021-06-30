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

import java.util.List;

import static com.rise_world.gematik.accesskeeper.pairingdienst.service.validation.Validations.BASE64URL_NOPADDING;
import static com.rise_world.gematik.accesskeeper.pairingdienst.service.validation.Validations.KEY_IDENTIFIER;

/**
 * Contains all data needed for an alternative authentication (i.e. an authentication using a pre-registered pairing).
 */
public class AuthenticationDataDTO {

    private static final Validation<String> EXPECTED_VERSION = Validations.expect("1.0");

    @JsonProperty("authentication_data_version")
    private String authenticationDataVersion;

    @JsonProperty("challenge_token")
    private String challengeToken;

    @JsonProperty("auth_cert")
    private String authCertificate;

    @JsonProperty("key_identifier")
    private String keyIdentifier;

    @JsonProperty("device_information")
    private DeviceInformationDTO deviceInformation;

    private List<String> amr;

    @JsonIgnore
    public boolean isValid() {
        return EXPECTED_VERSION.isValid(getAuthenticationDataVersion()) &&
               getChallengeToken() != null &&
               BASE64URL_NOPADDING.isValid(getAuthCertificate()) &&
               KEY_IDENTIFIER.isValid(getKeyIdentifier()) &&
               (getDeviceInformation() != null && getDeviceInformation().isValid()) &&
               (getAmr() != null && !getAmr().isEmpty());
    }

    public String getAuthenticationDataVersion() {
        return authenticationDataVersion;
    }

    public String getChallengeToken() {
        return challengeToken;
    }

    public String getAuthCertificate() {
        return authCertificate;
    }

    public String getKeyIdentifier() {
        return keyIdentifier;
    }

    public DeviceInformationDTO getDeviceInformation() {
        return deviceInformation;
    }

    public List<String> getAmr() {
        return amr;
    }

    public void setAuthenticationDataVersion(String authenticationDataVersion) {
        this.authenticationDataVersion = authenticationDataVersion;
    }

    public void setChallengeToken(String challengeToken) {
        this.challengeToken = challengeToken;
    }

    public void setAuthCertificate(String authCertificate) {
        this.authCertificate = authCertificate;
    }

    public void setKeyIdentifier(String keyIdentifier) {
        this.keyIdentifier = keyIdentifier;
    }

    public void setDeviceInformation(DeviceInformationDTO deviceInformation) {
        this.deviceInformation = deviceInformation;
    }

    public void setAmr(List<String> amr) {
        this.amr = amr;
    }
}

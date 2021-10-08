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
 * Decoded SignedPairingData
 */
public class SignedPairingDataDTO {

    public static final Validation<String> EXPECTED_VERSION = Validations.expect("1.0");

    @JsonProperty("pairing_data_version")
    private String pairingDataVersion;

    @JsonProperty("se_subject_public_key_info")
    private String sePublicKeyInfo;

    @JsonProperty("key_identifier")
    private String keyIdentifier;

    private String product;
    private String serialnumber;
    private String issuer;

    @JsonProperty("not_after")
    private Long notAfter;

    @JsonProperty("auth_cert_subject_public_key_info")
    private String pukCertificateInfo;

    @JsonIgnore
    public boolean isValid() {
        return ((this.getIssuer() != null) &&
            (this.getKeyIdentifier() != null) &&
            (this.getNotAfter() != null) &&
            (this.getPairingDataVersion() != null) &&
            (this.getProduct() != null) &&
            (this.getPukCertificateInfo() != null) &&
            (this.getSePublicKeyInfo() != null) &&
            (this.getSerialnumber() != null));
    }

    public String getPairingDataVersion() {
        return pairingDataVersion;
    }

    public void setPairingDataVersion(String pairingDataVersion) {
        this.pairingDataVersion = pairingDataVersion;
    }

    public String getSePublicKeyInfo() {
        return sePublicKeyInfo;
    }

    public void setSePublicKeyInfo(String sePublicKeyInfo) {
        this.sePublicKeyInfo = sePublicKeyInfo;
    }

    public String getKeyIdentifier() {
        return keyIdentifier;
    }

    public void setKeyIdentifier(String keyIdentifier) {
        this.keyIdentifier = keyIdentifier;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getSerialnumber() {
        return serialnumber;
    }

    public void setSerialnumber(String serialnumber) {
        this.serialnumber = serialnumber;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Long getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(Long notAfter) {
        this.notAfter = notAfter;
    }

    public String getPukCertificateInfo() {
        return pukCertificateInfo;
    }

    public void setPukCertificateInfo(String pukCertificateInfo) {
        this.pukCertificateInfo = pukCertificateInfo;
    }
}

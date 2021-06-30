/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.api.pairing;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SignedAuthenticationData")
public class SignedAuthenticationDataRequest {

    @Schema(required = true, name = "signed_authentication_data")
    @JsonProperty("signed_authentication_data")
    private String signedAuthenticationData;

    public SignedAuthenticationDataRequest(String signedAuthenticationData) {
        this.signedAuthenticationData = signedAuthenticationData;
    }

    public SignedAuthenticationDataRequest() {
        this(null);
    }

    public String getSignedAuthenticationData() {
        return signedAuthenticationData;
    }

    public void setSignedAuthenticationData(String signedAuthenticationData) {
        this.signedAuthenticationData = signedAuthenticationData;
    }
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.api.pairing;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthenticateResponse")
public class AuthenticationResponse {

    @Schema(name = "challenge_token", description = "challenge token extracted from authentication data")
    @JsonProperty("challenge_token")
    private String challengeToken;

    public AuthenticationResponse() {
    }

    public AuthenticationResponse(String challengeToken) {
        this.challengeToken = challengeToken;
    }

    public String getChallengeToken() {
        return challengeToken;
    }

    public void setChallengeToken(String challengeToken) {
        this.challengeToken = challengeToken;
    }
}

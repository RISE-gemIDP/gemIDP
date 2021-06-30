/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.dto;

public class RedeemedChallengeDTO {

    private String ssoToken;
    private String authCode;

    private String redirectUri;
    private String state;

    public RedeemedChallengeDTO(String ssoToken, String authCode, String redirectUri, String state) {
        this.ssoToken = ssoToken;
        this.authCode = authCode;
        this.redirectUri = redirectUri;
        this.state = state;
    }

    public String getSsoToken() {
        return ssoToken;
    }

    public String getAuthCode() {
        return authCode;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getState() {
        return state;
    }
}

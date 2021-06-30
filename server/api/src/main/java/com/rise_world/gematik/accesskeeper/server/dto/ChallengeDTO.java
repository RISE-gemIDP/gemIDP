/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.dto;

public class ChallengeDTO {

    private String challenge;
    private UserConsentDTO userConsent;

    public ChallengeDTO(String challenge, UserConsentDTO userConsent) {
        this.challenge = challenge;
        this.userConsent = userConsent;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public UserConsentDTO getUserConsent() {
        return userConsent;
    }

    public void setUserConsent(UserConsentDTO userConsent) {
        this.userConsent = userConsent;
    }
}

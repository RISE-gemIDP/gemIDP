/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.entity;

import java.sql.Timestamp;

public class ExtSessionEntity {

    private Timestamp creationTime;
    private String state;
    private String idpIss;
    private String clientId;
    private String clientRedirectUri;
    private String clientCodeChallenge;
    private String clientScope;
    private String clientState;
    private String clientNonce;
    private String idpCodeVerifier;
    private String idpNonce;

    public Timestamp getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Timestamp creationTime) {
        this.creationTime = creationTime;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getIdpIss() {
        return idpIss;
    }

    public void setIdpIss(String idpIss) {
        this.idpIss = idpIss;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientRedirectUri() {
        return clientRedirectUri;
    }

    public void setClientRedirectUri(String clientRedirectUri) {
        this.clientRedirectUri = clientRedirectUri;
    }

    public String getClientCodeChallenge() {
        return clientCodeChallenge;
    }

    public void setClientCodeChallenge(String clientCodeChallenge) {
        this.clientCodeChallenge = clientCodeChallenge;
    }

    public String getClientScope() {
        return clientScope;
    }

    public void setClientScope(String clientScope) {
        this.clientScope = clientScope;
    }

    public String getClientState() {
        return clientState;
    }

    public void setClientState(String clientState) {
        this.clientState = clientState;
    }

    public String getClientNonce() {
        return clientNonce;
    }

    public void setClientNonce(String clientNonce) {
        this.clientNonce = clientNonce;
    }

    public String getIdpCodeVerifier() {
        return idpCodeVerifier;
    }

    public void setIdpCodeVerifier(String idpCodeVerifier) {
        this.idpCodeVerifier = idpCodeVerifier;
    }

    public String getIdpNonce() {
        return idpNonce;
    }

    public void setIdpNonce(String idpNonce) {
        this.idpNonce = idpNonce;
    }
}

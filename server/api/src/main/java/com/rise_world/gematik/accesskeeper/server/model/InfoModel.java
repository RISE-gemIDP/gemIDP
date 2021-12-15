/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class InfoModel {

    @JsonProperty("issuer_ti")
    private String issuerTi;
    @JsonProperty("issuer_internet")
    private String issuerInet;

    private String pairingEndpoint;
    private String salt;

    private Long challengeExpires;
    private Long authCodeExpires;

    private List<Scope> scopes;
    @JsonProperty("public_clients")
    private List<Client> publicClients;
    private List<Fachdienst> fachdienste;
    @JsonProperty("sektor_apps")
    private List<SektorApp> sektorApps;

    public InfoModel() {
    }

    public InfoModel(InfoModel src) {
        this.issuerTi = src.issuerTi;
        this.issuerInet = src.issuerInet;
        this.pairingEndpoint = src.pairingEndpoint;
        this.challengeExpires = src.challengeExpires;
        this.authCodeExpires = src.authCodeExpires;
        this.getScopes().addAll(src.getScopes());
        this.getPublicClients().addAll(src.getPublicClients());
        this.getFachdienste().addAll(src.getFachdienste());
        this.getSektorApps().addAll(src.getSektorApps());
    }

    public String getIssuerTi() {
        return issuerTi;
    }

    public void setIssuerTi(String issuerTi) {
        this.issuerTi = issuerTi;
    }

    public String getIssuerInet() {
        return issuerInet;
    }

    public void setIssuerInet(String issuerInet) {
        this.issuerInet = issuerInet;
    }

    public String getPairingEndpoint() {
        return pairingEndpoint;
    }

    public void setPairingEndpoint(String pairingEndpoint) {
        this.pairingEndpoint = pairingEndpoint;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public Long getChallengeExpires() {
        return challengeExpires;
    }

    public void setChallengeExpires(Long challengeExpires) {
        this.challengeExpires = challengeExpires;
    }

    public Long getAuthCodeExpires() {
        return authCodeExpires;
    }

    public void setAuthCodeExpires(Long authCodeExpires) {
        this.authCodeExpires = authCodeExpires;
    }

    /**
     * Returns the scope list
     *
     * @return the list of configured scopes
     */
    public List<Scope> getScopes() {
        if (scopes == null) {
            scopes = new ArrayList<>();
        }
        return scopes;
    }

    public void setScopes(List<Scope> scopes) {
        this.scopes = scopes;
    }

    /**
     * Returns the public client list
     *
     * @return the list of configured public clients
     */
    public List<Client> getPublicClients() {
        if (publicClients == null) {
            publicClients = new ArrayList<>();
        }
        return publicClients;
    }

    public void setPublicClients(List<Client> publicClients) {
        this.publicClients = publicClients;
    }


    /**
     * Returns the fachdienst list
     *
     * @return the list of configured fachdienste
     */
    public List<Fachdienst> getFachdienste() {
        if (fachdienste == null) {
            fachdienste = new ArrayList<>();
        }
        return fachdienste;
    }

    public void setFachdienste(List<Fachdienst> fachdienste) {
        this.fachdienste = fachdienste;
    }

    /**
     * Returns the sektor idp list. If no sektor idps are configured, an empty list will be returned.
     *
     * @return the list of configured sektor idps
     */
    public List<SektorApp> getSektorApps() {
        if (sektorApps == null) {
            sektorApps = new ArrayList<>();
        }
        return sektorApps;
    }

    public void setSektorApps(List<SektorApp> sektorApps) {
        this.sektorApps = sektorApps;
    }

}

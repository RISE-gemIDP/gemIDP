/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.model;

import java.util.Objects;

public class Fachdienst {

    private String id;
    private String aud;
    private String sectorIdentifier;
    private Long tokenTimeout;
    private Long ssoTokenExpires;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAud() {
        return aud;
    }

    public void setAud(String aud) {
        this.aud = aud;
    }

    public String getSectorIdentifier() {
        return sectorIdentifier;
    }

    public void setSectorIdentifier(String sectorIdentifier) {
        this.sectorIdentifier = sectorIdentifier;
    }

    public Long getTokenTimeout() {
        return tokenTimeout;
    }

    public void setTokenTimeout(Long tokenTimeout) {
        this.tokenTimeout = tokenTimeout;
    }

    public Long getSsoTokenExpires() {
        return ssoTokenExpires;
    }

    public void setSsoTokenExpires(Long ssoTokenExpires) {
        this.ssoTokenExpires = ssoTokenExpires;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Fachdienst)) {
            return false;
        }
        Fachdienst other = (Fachdienst) obj;
        return Objects.equals(id, other.id);
    }
}

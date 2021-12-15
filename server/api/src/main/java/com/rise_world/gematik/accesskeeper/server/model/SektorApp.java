/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class SektorApp {

    private String id;
    private String name;

    @JsonProperty("idp_iss")
    private String idpIss;

    @JsonProperty("kk_app_uri")
    private String kkAppUri;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdpIss() {
        return idpIss;
    }

    public void setIdpIss(String idpIss) {
        this.idpIss = idpIss;
    }

    public String getKkAppUri() {
        return kkAppUri;
    }

    public void setKkAppUri(String kkAppUri) {
        this.kkAppUri = kkAppUri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SektorApp sektorApp = (SektorApp) o;
        return Objects.equals(id, sektorApp.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

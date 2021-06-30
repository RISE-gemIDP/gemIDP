/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.model;

import java.util.Map;
import java.util.Objects;

public class Scope {

    private String id;
    private String description;
    private String fachdienst;
    private Map<String, String> claims;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFachdienst() {
        return fachdienst;
    }

    public void setFachdienst(String fachdienst) {
        this.fachdienst = fachdienst;
    }

    public Map<String, String> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, String> claims) {
        this.claims = claims;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Scope scope = (Scope) o;
        return Objects.equals(id, scope.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

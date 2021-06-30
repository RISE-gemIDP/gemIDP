/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Client {

    private String id;
    private String name;
    private Long tokenTimeout;
    private boolean needsSsoToken;

    private List<String> validRedirectUris;

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

    public Long getTokenTimeout() {
        return tokenTimeout;
    }

    public void setTokenTimeout(Long tokenTimeout) {
        this.tokenTimeout = tokenTimeout;
    }

    public boolean isNeedsSsoToken() {
        return needsSsoToken;
    }

    public void setNeedsSsoToken(boolean needsSsoToken) {
        this.needsSsoToken = needsSsoToken;
    }

    public List<String> getValidRedirectUris() {
        if (validRedirectUris == null) {
            validRedirectUris = new ArrayList<>();
        }
        return validRedirectUris;
    }

    public void setValidRedirectUris(List<String> validRedirectUris) {
        this.validRedirectUris = validRedirectUris;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Client client = (Client) o;
        return Objects.equals(id, client.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

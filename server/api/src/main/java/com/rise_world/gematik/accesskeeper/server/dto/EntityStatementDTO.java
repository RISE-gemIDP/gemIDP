/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.dto;

import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class EntityStatementDTO extends OpenidProviderDTO {

    private String tokenEndpoint;
    private String pushedAuthorizationRequestEndpoint;
    private String authorizationEndpoint;
    private Instant createdAt;
    private Map<String, JsonWebKey> keys = new HashMap<>();
    private long exp;

    public EntityStatementDTO(String issuer, String organizationName, String logoUri, boolean pkv, String tokenEndpoint, String pushedAuthorizationRequestEndpoint,
                              String authorizationEndpoint, Instant createdAt) {
        super(issuer, organizationName, logoUri, pkv);
        this.tokenEndpoint = tokenEndpoint;
        this.pushedAuthorizationRequestEndpoint = pushedAuthorizationRequestEndpoint;
        this.authorizationEndpoint = authorizationEndpoint;
        this.createdAt = createdAt;
    }

    public EntityStatementDTO(String issuer, String organizationName, String logoUri, boolean pkv) {
        super(issuer, organizationName, logoUri, pkv);
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getPushedAuthorizationRequestEndpoint() {
        return pushedAuthorizationRequestEndpoint;
    }

    public void setPushedAuthorizationRequestEndpoint(String pushedAuthorizationRequestEndpoint) {
        this.pushedAuthorizationRequestEndpoint = pushedAuthorizationRequestEndpoint;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authEndpoint) {
        this.authorizationEndpoint = authEndpoint;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, JsonWebKey> getKeys() {
        return keys;
    }

    public void setKeys(Map<String, JsonWebKey> keys) {
        this.keys = keys;
    }

    public long getExp() {
        return exp;
    }

    public void setExp(long exp) {
        this.exp = exp;
    }
}

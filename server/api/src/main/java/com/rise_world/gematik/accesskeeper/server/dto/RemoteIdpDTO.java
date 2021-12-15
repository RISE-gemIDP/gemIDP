/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.dto;

import com.rise_world.gematik.accesskeeper.server.model.SektorApp;

import java.security.PublicKey;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class RemoteIdpDTO {

    private Instant createdAt;
    private SektorApp appConfig;

    private String issuer;
    private String tokenEndpoint;
    // map keyId -> public key
    private Map<String, PublicKey> webKeys = new HashMap<>();

    public RemoteIdpDTO(Instant createdAt, SektorApp appConfig, String issuer, String tokenEndpoint) {
        this.createdAt = createdAt;
        this.appConfig = appConfig;
        this.issuer = issuer;
        this.tokenEndpoint = tokenEndpoint;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public SektorApp getAppConfig() {
        return appConfig;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public Map<String, PublicKey> getWebKeys() {
        return webKeys;
    }
}

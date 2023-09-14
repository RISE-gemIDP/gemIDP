/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.dto;

public class OpenidProviderDTO {

    private final String issuer;
    private final String organizationName;
    private final String logoUri;
    private final boolean pkv;

    public OpenidProviderDTO(String issuer, String organizationName, String logoUri, boolean pkv) {
        this.issuer = issuer;
        this.organizationName = organizationName;
        this.logoUri = logoUri;
        this.pkv = pkv;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getLogoUri() {
        return logoUri;
    }

    public boolean isPkv() {
        return pkv;
    }
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.dto;

public enum Endpoint {
    AUTH("idp_sig"),
    // @AFO: A_20691-01 - Der Schl&uuml;ssel disc_sig wird dem Discovery Endpoint zugeordnet
    DISC("disc_sig"),
    TOKEN("idp_sig"),
    // @AFO: A_22266 - Der Schl&uuml;ssel idp_sig_sek wird dem Token private_key_jwt zugeordnet
    EXT_AUTH("idp_sig_sek");

    private static final String PRIVATE_KEY_PREFIX = "prk_";
    private String keyName;

    Endpoint(String keyName) {
        this.keyName = keyName;
    }

    public String getKeyName() {
        return this.keyName;
    }

    public String getPrivateKeyName() {
        return PRIVATE_KEY_PREFIX + this.keyName;
    }
}

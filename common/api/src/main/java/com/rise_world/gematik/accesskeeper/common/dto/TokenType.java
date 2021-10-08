/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.dto;

public enum TokenType {

    CHALLENGE("challenge", false, true),
    AUTH_CODE("code", true, false),
    SSO("sso_token", true, false),
    ID("id_token", false, false),
    ACCESS("access_token", false, true), // @AFO: A_21445 AccessToken wird mit ECDH_ES_DIRECT entschl&uuml;sselt
    KEY_VERIFIER("key_verifier", false, true),
    REGISTRATION_INFO("registration_info", false, true),
    ALTERNATIVE_AUTH_DATA("alternative_auth_data", false, true)
    ;

    private final String id;
    private final boolean directDecrypt;
    private final boolean ecdhEsDecrypt;

    TokenType(String id, boolean directDecrypt, boolean ecdhEsDecrypt) {
        if (directDecrypt && ecdhEsDecrypt) {
            throw new IllegalArgumentException("directDecrypt and ecdhEsDecrypt are mutually exclusive");
        }

        this.id = id;
        this.directDecrypt = directDecrypt;
        this.ecdhEsDecrypt = ecdhEsDecrypt;
    }

    public String getId() {
        return id;
    }

    public boolean isDirectDecrypt() {
        return directDecrypt;
    }

    public boolean isEcdhEsDecrypt() {
        return ecdhEsDecrypt;
    }
}

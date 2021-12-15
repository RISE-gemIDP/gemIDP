/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common;

public class OAuth2Constants {

    // oauth constants
    public static final String SCOPE_OPENID = "openid";
    public static final String GRANT_TYPE_CODE = "authorization_code";
    public static final String RESPONSE_TYPE_CODE = "code";

    // PKCE constants
    public static final String PKCE_METHOD_S256 = "S256";

    // acr level of assurance
    public static final String ACR_LOA_HIGH = "gematik-ehealth-loa-high";

    public static final String EXTERNAL_CLIENT_ID = "zentraler-idp-dienst";

    // amr methods
    public static final String AMR_MULTI_FACTOR_AUTH = "mfa";
    public static final String AMR_SMART_CARD = "sc";
    public static final String AMR_PIN = "pin";

    private OAuth2Constants() {
    }

}

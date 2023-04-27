/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.crypt;

public class KeyConstants {

    public static final String PUK_IDP_ENC = "puk_idp_enc";
    public static final String PUK_IDP_SIG = "puk_idp_sig";
    public static final String PUK_DISC_SIG = "puk_disc_sig";
    public static final String PUK_IDP_SIG_SEK = "puk_idp_sig_sek";
    public static final String PUK_FEDMASTER_SIG = "puk_fedmaster_sig";

    private KeyConstants() {
        // avoid instantiation
    }
}

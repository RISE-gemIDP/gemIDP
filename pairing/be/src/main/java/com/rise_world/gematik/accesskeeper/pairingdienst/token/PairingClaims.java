/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.token;

public class PairingClaims {

    public static final String PAIRING_DATA_CLAIM_VERSION = "pairing_data_version";
    public static final String PAIRING_DATA_CLAIM_SE_SUBJECT_PUBLIC_KEY_INFO = "se_subject_public_key_info";
    public static final String PAIRING_DATA_CLAIM_KEY_IDENTIFIER = "key_identifier";
    public static final String PAIRING_DATA_CLAIM_DEVICE_PRODUCT = "product";
    public static final String PAIRING_DATA_CLAIM_SERIALNUMBER = "serialnumber";
    public static final String PAIRING_DATA_CLAIM_ISSUER = "issuer";
    public static final String PAIRING_DATA_CLAIM_NOT_AFTER = "not_after";
    public static final String PAIRING_DATA_CLAIM_AUTH_CERT_SUBJECT_PUBLIC_KEY_INFO = "auth_cert_subject_public_key_info";

    private PairingClaims() {
    }
}

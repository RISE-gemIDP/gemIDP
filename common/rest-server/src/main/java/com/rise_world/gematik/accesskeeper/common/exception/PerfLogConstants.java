/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.exception;

public class PerfLogConstants {
    public static final String HEADER_PERF_DIENST_OPERATION = "perf_dienst_operation";
    public static final String HEADER_PERF_ERRORCODE = "perf_errorcode";

    public static final String IDP_PERF_OCSP_OPERATION = "OCSP-Abfrage";
    public static final String IDP_PERF_OCSP_OPERATION_FAILED = IDP_PERF_OCSP_OPERATION + ".failed";
    public static final String IDP_PERF_FAILED = "IDP.failed";

    public static final String IDP_PERF_SEK_IDP_OPERATION = "Sek-IDP-Abfrage";

    public static final int CS_OCSP_CHECK_REVOCATION_ERROR = 1029;
    public static final int CS_CERTHASH_EXTENSION_MISSING = 1040;
    public static final int CS_CERTHASH_MISMATCH = 1041;
    public static final int CS_OCSP_STATUS_ERROR_MALFORMED_REQUEST = 10581;
    public static final int CS_OCSP_NOT_AVAILABLE = 1032;
    public static final int CS_OCSP_SIGNATURE_ERROR = 1031;
    public static final int CS_OCSP_NO_CERTIFICATE = 1030;

    public static final int IDP_PERF_ERROR_NO_RESPONSE = 79001;
    public static final int IDP_PERF_ERROR_WRONG_SIGNATURE = 79879;
    public static final int IDP_PERF_ERROR_WRONG_DATA = 79875;
    public static final int IDP_PERF_ERROR_INVALID_RESPONSE = 79881;
    public static final int IDP_PERF_CERT_MISSING = 79873;
    public static final int IDP_PERF_INTERNAL_IDP_ERROR = 79000;

    public static final int IDP_PERF_SEK_IDP_ERROR_NO_RESPONSE = 79101;
    public static final int IDP_PERF_SEK_IDP_ERROR_INVALID_RESPONSE = 79102;


    private PerfLogConstants() {
        // private constructor for Sonar
    }
}

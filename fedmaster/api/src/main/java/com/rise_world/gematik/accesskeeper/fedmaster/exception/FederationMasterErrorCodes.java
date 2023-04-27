/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.exception;

import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;

import static com.rise_world.gematik.accesskeeper.common.exception.OAuth2Error.INVALID_REQUEST;

public class FederationMasterErrorCodes {

    public static final ErrorMessage FED_UNKNOWN_ISS = new ErrorMessage(6000, INVALID_REQUEST, "Issuer entspricht nicht dem Entity Identifier des Federation Masters");
    public static final ErrorMessage FED_INVALID_ISS = new ErrorMessage(6001, INVALID_REQUEST, "Issuer ist ung\u00fcltig");
    public static final ErrorMessage FED_INVALID_SUB = new ErrorMessage(6010, INVALID_REQUEST, "Subject ist ung\u00fcltig");
    public static final ErrorMessage FED_UNKNOWN_SUB = new ErrorMessage(6011, INVALID_REQUEST, "Subject ist unbekannt", 404);
    public static final ErrorMessage FED_INVALID_AUD = new ErrorMessage(6020, INVALID_REQUEST, "Audience ist ung\u00fcltig");


    private FederationMasterErrorCodes() {
        // avoid instantiation
    }
}

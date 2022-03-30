/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.exception;

public class ErrorCodes {

    // common errors 1xxx
    public static final ErrorMessage COMMON_MISSING_CLIENT_ID = new ErrorMessage(1002, OAuth2Error.INVALID_REQUEST, "client_id wurde nicht \u00fcbermittelt");
    public static final ErrorMessage COMMON_MISSING_REDIRECT = new ErrorMessage(1004, OAuth2Error.INVALID_REQUEST, "redirect_uri wurde nicht \u00fcbermittelt");
    public static final ErrorMessage COMMON_MISSING_SCOPE = new ErrorMessage(1005, OAuth2Error.INVALID_REQUEST, "scope wurde nicht \u00fcbermittelt", 302);
    public static final ErrorMessage COMMON_INVALID_REDIRECT_URI = new ErrorMessage(1020, OAuth2Error.INVALID_REQUEST, "redirect_uri ist ung\u00fcltig");
    public static final ErrorMessage COMMON_INVALID_SCOPE = new ErrorMessage(1022, OAuth2Error.INVALID_SCOPE, "scope ist ung\u00fcltig");
    public static final ErrorMessage COMMON_UNKNOWN_FD = new ErrorMessage(1030, OAuth2Error.INVALID_SCOPE, "Fachdienst ist unbekannt");

    public static final ErrorMessage SERVER_ERROR = new ErrorMessage(1500, null, "Allgemeiner Serverfehler", 500);

    // auth endpoint errors 2xxx
    // @AFO A_21433  - Definition von VAL.1
    public static final ErrorMessage VAL1_ALT_AUTH_FAILED = new ErrorMessage(
        2000, OAuth2Error.ACCESS_DENIED, "Die Authentifizierung mit einem alternativen Authentifizierungsmittel konnte nicht erfolgreich durchgef\u00fchrt werden.", 400);

    public static final ErrorMessage AUTH_MISSING_CLIENT_SIGNATURE = new ErrorMessage(2001, OAuth2Error.INVALID_REQUEST, "Der Request wurde nicht signiert");
    public static final ErrorMessage AUTH_MISSING_STATE_PARAMETER = new ErrorMessage(2002, OAuth2Error.INVALID_REQUEST, "state wurde nicht \u00fcbermittelt", 302);
    public static final ErrorMessage AUTH_WRONG_ALGO = new ErrorMessage(2003, OAuth2Error.INVALID_REQUEST, "Es wurde ein ung\u00fcltiger Algorithmus verwendet");
    public static final ErrorMessage AUTH_MISSING_RESPONSE_TYPE = new ErrorMessage(2004, OAuth2Error.INVALID_REQUEST, "response_type wurde nicht \u00fcbermittelt", 302);
    public static final ErrorMessage AUTH_INVALID_RESPONSE_TYPE = new ErrorMessage(2005, OAuth2Error.UNSUPPORTED_RESPONSE_TYPE, "response_type wird nicht unterst\u00fctzt", 302);
    public static final ErrorMessage AUTH_INVALID_STATE_PARAMETER = new ErrorMessage(2006, OAuth2Error.INVALID_REQUEST, "state ist ung\u00fcltig", 302);
    public static final ErrorMessage AUTH_INVALID_NONCE_PARAMETER = new ErrorMessage(2007, OAuth2Error.INVALID_REQUEST, "nonce ist ung\u00fcltig", 302);
    public static final ErrorMessage AUTH_INVALID_CHALLENGE_METHOD = new ErrorMessage(2008, OAuth2Error.INVALID_REQUEST, "code_challenge_method ist ung\u00fcltig", 302);
    // @AFO: A_20434 - fehlende code_challenge
    public static final ErrorMessage AUTH_MISSING_CODE_CHALLENGE = new ErrorMessage(2009, OAuth2Error.INVALID_REQUEST, "code_challenge wurde nicht \u00fcbermittelt", 302);
    public static final ErrorMessage AUTH_INVALID_CODE_CHALLENGE = new ErrorMessage(2010, OAuth2Error.INVALID_REQUEST, "code_challenge ist ung\u00fcltig", 302);
    public static final ErrorMessage AUTH_DUPLICATE_PARAMETERS = new ErrorMessage(2011, OAuth2Error.INVALID_REQUEST, "Requestparameter mehrfach vorhanden");
    public static final ErrorMessage AUTH_INVALID_CLIENT = new ErrorMessage(2012, OAuth2Error.INVALID_REQUEST, "client_id ist ung\u00fcltig");
    public static final ErrorMessage AUTH_INVALID_CLIENT_SIGNATURE = new ErrorMessage(2013, OAuth2Error.INVALID_REQUEST, "Der Request besitzt keine g\u00fcltige Signatur");
    public static final ErrorMessage AUTH_MISSING_SERVER_SIGNATURE = new ErrorMessage(2014, OAuth2Error.INVALID_REQUEST, "Der Request wurde nicht vom IDP signiert");
    public static final ErrorMessage AUTH_WRONG_SERVER_ALGO = new ErrorMessage(2015, OAuth2Error.INVALID_REQUEST, "IDP-Signaturalgorithmus ist ung\u00fcltig");
    public static final ErrorMessage AUTH_INVALID_SERVER_SIGNATURE = new ErrorMessage(2016, OAuth2Error.INVALID_REQUEST, "Der Request besitzt keine g\u00fcltige IDP-Signatur");

    // auth certificate errors
    public static final ErrorMessage AUTH_INVALID_X509_CERT = new ErrorMessage(2020, OAuth2Error.INVALID_REQUEST, "Das AUT Zertifikat ist ung\u00fcltig");

    public static final ErrorMessage AUTH_OCSP_ERROR_NO_RESPONSE = new ErrorMessage(2021, OAuth2Error.INVALID_REQUEST, "Keine Antwort des OCSP oder Timeout");

    // challenge / redeem sso token errors
    public static final ErrorMessage AUTH_INVALID_CHALLENGE = new ErrorMessage(2030, OAuth2Error.INVALID_REQUEST, "Challenge ist ung\u00fcltig");
    public static final ErrorMessage AUTH_CHALLENGE_MISSING_EXPIRY = new ErrorMessage(2031, OAuth2Error.INVALID_REQUEST, "exp wurde nicht \u00fcbermittelt");
    public static final ErrorMessage AUTH_CHALLENGE_EXPIRED = new ErrorMessage(2032, OAuth2Error.INVALID_REQUEST, "Challenge ist abgelaufen");

    // @AFO: A_20949 - textueller Hinweis, dass neuerliche Authentisierung notwendig ist
    public static final ErrorMessage AUTH_INVALID_SSO_TOKEN = new ErrorMessage(2040, OAuth2Error.ACCESS_DENIED,
        "SSO_TOKEN nicht valide, bitte um neuerliche Authentisierung");
    // @AFO: A_20949 - textueller Hinweis, dass neuerliche Authentisierung notwendig ist
    public static final ErrorMessage AUTH_SSO_TOKEN_CHALLENGE_MISMATCH = new ErrorMessage(2041, OAuth2Error.ACCESS_DENIED,
        "SSO_TOKEN und Challenge passen nicht zusammen, bitte um neuerliche Authentisierung");
    // @AFO: A_20949 - textueller Hinweis, dass neuerliche Authentisierung notwendig ist
    public static final ErrorMessage AUTH_REQUESTED_CLAIMS_NOT_CONSENTED = new ErrorMessage(2042, OAuth2Error.ACCESS_DENIED,
        "Nicht alle angeforderten Claims sind im UserConsent vorhanden, bitte um neuerliche Authentisierung");
    // @AFO: A_20949 - textueller Hinweis, dass neuerliche Authentisierung notwendig ist
    public static final ErrorMessage AUTH_SSO_TOKEN_NOT_CONFIGURED = new ErrorMessage(2043, OAuth2Error.ACCESS_DENIED,
        "Der Client ist nicht berechtigt einen SSO_TOKEN einzul\u00f6sen, bitte um neuerliche Authentisierung");

    public static final ErrorMessage AUTH_ENCRYPTION = new ErrorMessage(2050, OAuth2Error.SERVER_ERROR, "Verschl\u00fcsselung nicht m\u00f6glich");

    // in case of internal server errors which are sent as redirect
    public static final ErrorMessage AUTH_INTERNAL_SERVER_ERROR = new ErrorMessage(2100, OAuth2Error.SERVER_ERROR, "Ein Fehler ist aufgetreten", 302);


    // token endpoint errors 3xxx
    public static final ErrorMessage TOKEN_BROKEN_CODE_CHALLENGE = new ErrorMessage(3000, OAuth2Error.INVALID_GRANT, "code_verifier stimmt nicht mit code_challenge \u00fcberein");
    public static final ErrorMessage TOKEN_MISSING_CLAIMS = new ErrorMessage(3001, OAuth2Error.INVALID_GRANT, "Claims unvollst\u00e4ndig im Authorization Code");
    public static final ErrorMessage TOKEN_DUPLICATE_PARAMETERS = new ErrorMessage(3002, OAuth2Error.INVALID_REQUEST, "Tokenendpunkt Requestparameter mehrfach vorhanden");
    public static final ErrorMessage TOKEN_MISSING_CODE_VERIFIER = new ErrorMessage(3004, OAuth2Error.INVALID_REQUEST, "code_verifier wurde nicht \u00fcbermittelt");
    public static final ErrorMessage TOKEN_MISSING_AUTH_CODE = new ErrorMessage(3005, OAuth2Error.INVALID_REQUEST, "Authorization Code wurde nicht \u00fcbermittelt");
    public static final ErrorMessage TOKEN_MISSING_GRANT = new ErrorMessage(3006, OAuth2Error.INVALID_REQUEST, "grant_type wurde nicht \u00fcbermittelt");
    public static final ErrorMessage TOKEN_INVALID_CLIENT = new ErrorMessage(3007, OAuth2Error.INVALID_CLIENT, "client_id ist ung\u00fcltig");
    public static final ErrorMessage TOKEN_BROKEN_SIGNATURE = new ErrorMessage(3010, OAuth2Error.INVALID_GRANT, "Authorization Code Signatur ung\u00fcltig");
    public static final ErrorMessage TOKEN_AUTH_CODE_EXPIRED = new ErrorMessage(3011, OAuth2Error.INVALID_GRANT, "Authorization Code ist abgelaufen");
    public static final ErrorMessage TOKEN_MISSING_EXPIRY = new ErrorMessage(3012, OAuth2Error.INVALID_GRANT, "exp wurde nicht \u00fcbermittelt");
    public static final ErrorMessage TOKEN_INVALID_AUTH_CODE = new ErrorMessage(3013, OAuth2Error.INVALID_GRANT, "Authorization Code ist nicht lesbar");
    public static final ErrorMessage TOKEN_UNSUPPORTED_GRANT = new ErrorMessage(3014, OAuth2Error.UNSUPPORTED_GRANT_TYPE, "grant_type wird nicht unterst\u00fctzt");
    public static final ErrorMessage TOKEN_INVALID_CODE_VERIFIER = new ErrorMessage(3016, OAuth2Error.INVALID_REQUEST, "code_verifier ist ung\u00fcltig");
    public static final ErrorMessage TOKEN_MISSING_KEY_VERIFIER = new ErrorMessage(3020, OAuth2Error.INVALID_REQUEST, "key_verifier wurde nicht \u00fcbermittelt");
    public static final ErrorMessage TOKEN_INVALID_KEY_VERIFIER = new ErrorMessage(3021, OAuth2Error.INVALID_REQUEST, "key_verifier ist nicht lesbar");
    public static final ErrorMessage TOKEN_MISSING_TOKEN_KEY = new ErrorMessage(3022, OAuth2Error.INVALID_REQUEST, "token_key wurde nicht \u00fcbermittelt");
    public static final ErrorMessage TOKEN_INVALID_TOKEN_KEY = new ErrorMessage(3023, OAuth2Error.INVALID_REQUEST, "token_key ist nicht lesbar");

    // pairing endpoint errors 4xxx
    public static final ErrorMessage AC3_DEREGISTER_USER_ERROR = new ErrorMessage(4000, OAuth2Error.INVALID_REQUEST,
        "Der Auftrag zur Deaktivierung des Pairings konnte nicht angenommen werden.", 400);
    public static final ErrorMessage REG1_CLIENT_ERROR = new ErrorMessage(4001, OAuth2Error.ACCESS_DENIED,
        "Der Zugriff auf den Dienst kann nicht gew\u00e4hrt werden.", 403);
    public static final ErrorMessage REG2_DEVICE_BLACKLISTED = new ErrorMessage(4002, OAuth2Error.ACCESS_DENIED,
        "Das verwendete Ger\u00e4t ist nicht f\u00fcr die Authentisierung geeignet.", 400);
    public static final ErrorMessage REG3_REGISTRATION_SERVER_ERROR = new ErrorMessage(4003, OAuth2Error.SERVER_ERROR,
        "Der erzeugte Schl\u00fcssel konnte aufgrund eines internen Fehlers nicht registriert werden", 500);
    public static final ErrorMessage REG4_DUPLICATE_PAIRING = new ErrorMessage(4004, OAuth2Error.INVALID_REQUEST,
        "Der erzeugte Schl\u00fcssel konnte aufgrund eines bestehenden Eintrags nicht registriert werden.", 409);
    public static final ErrorMessage AC2_NO_DATA_ACCESS = new ErrorMessage(4005, OAuth2Error.SERVER_ERROR, "Die Registrierungsdaten konnten nicht bezogen werden.", 500);

    // external auth endpoint errors 5xxx
    public static final ErrorMessage EXTAUTH_MISSING_KK_APP = new ErrorMessage(5000, OAuth2Error.INVALID_REQUEST, "kk_app_id wurde nicht \u00fcbermittelt");
    public static final ErrorMessage EXTAUTH_UNKNOWN_KK_APP = new ErrorMessage(5001, OAuth2Error.INVALID_REQUEST, "kk_app_id ist ung\u00fcltig");
    public static final ErrorMessage EXTAUTH_IDP_NOT_AVAILABLE = new ErrorMessage(5002, OAuth2Error.TEMP_UNAVAILABLE, "sektoraler IDP ist nicht erreichbar");
    public static final ErrorMessage EXTAUTH_UNKNOWN_SESSION = new ErrorMessage(5010, OAuth2Error.ACCESS_DENIED, "Session ist ung\u00fcltig");

    public static final ErrorMessage EXTAUTH_MISSING_CODE = new ErrorMessage(5011, OAuth2Error.INVALID_REQUEST, "code wurde nicht \u00fcbermittelt");
    public static final ErrorMessage EXTAUTH_MISSING_STATE = new ErrorMessage(5012, OAuth2Error.INVALID_REQUEST, "state wurde nicht \u00fcbermittelt");
    public static final ErrorMessage EXTAUTH_MISSING_KKA_REDIRECT_URI = new ErrorMessage(5013, OAuth2Error.INVALID_REQUEST, "kk_app_redirect_uri wurde nicht \u00fcbermittelt");

    public static final ErrorMessage EXTAUTH_INVALID_CODE = new ErrorMessage(5014, OAuth2Error.INVALID_REQUEST, "code ist ung\u00fcltig");
    public static final ErrorMessage EXTAUTH_INVALID_STATE = new ErrorMessage(5015, OAuth2Error.INVALID_REQUEST, "state ist ung\u00fcltig");
    public static final ErrorMessage EXTAUTH_INVALID_KKA_REDIRECT_URI = new ErrorMessage(5016, OAuth2Error.INVALID_REQUEST, "kk_app_redirect_uri ist ung\u00fcltig");

    public static final ErrorMessage EXTAUTH_FAILED_TO_REDEEM = new ErrorMessage(5020, OAuth2Error.INVALID_GRANT, "Id-Token konnte nicht abgerufen werden");
    public static final ErrorMessage EXTAUTH_INVALID_ID_TOKEN = new ErrorMessage(5021, OAuth2Error.INVALID_GRANT, "Id-Token ist ung\u00fcltig");

    // discovery endpoint error 6xxx

    private ErrorCodes() {
        // avoid instantiation
    }
}

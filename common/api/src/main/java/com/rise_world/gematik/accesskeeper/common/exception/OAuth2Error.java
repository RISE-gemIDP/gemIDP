/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.exception;

/**
 * Contains the OAuth2 error codes as defined in RFC6749 4.1.2.1 (https://tools.ietf.org/html/rfc6749)
 */
public enum OAuth2Error {

    /**
     * The request is missing a required parameter, includes an invalid parameter value, includes a parameter more than once, or is otherwise malformed.
     */
    INVALID_REQUEST("invalid_request"),
    /**
     * The client is not authorized to request an authorization code using this method.
     */
    UNAUTHORIZED_CLIENT("unauthorized_client"),
    /**
     * The resource owner or authorization server denied the request.
     */
    ACCESS_DENIED("access_denied"),
    /**
     * The authorization server does not support obtaining an authorization code using this method.
     */
    UNSUPPORTED_RESPONSE_TYPE("unsupported_response_type"),
    /**
     * The requested scope is invalid, unknown, or malformed.
     */
    INVALID_SCOPE("invalid_scope"),
    /**
     * The authorization server encountered an unexpected condition that prevented it from fulfilling the request.
     * (This error code is needed because a 500 Internal Server Error HTTP status code cannot be returned to the client via an HTTP redirect.)
     */
    SERVER_ERROR("server_error"),
    /**
     * The authorization server is currently unable to handle the request due to a temporary overloading or maintenance of the server.
     * (This error code is needed because a 503 Service Unavailable HTTP status code cannot be returned to the client via an HTTP redirect.)
     */
    TEMP_UNAVAILABLE("temporarily_unavailable"),
    /**
     * The authorization grant type is not supported by the authorization server.
     */
    UNSUPPORTED_GRANT_TYPE("unsupported_grant_type"),
    /**
     * The provided authorization grant (e.g., authorization code, resource owner
     * credentials) or refresh token is invalid, expired, revoked, does not match
     * the redirection URI used in the authorization request, or was issued to
     * another client.
     */
    INVALID_GRANT("invalid_grant"),
    /**
     * Client authentication failed (e.g., unknown client, no client authentication
     * included, or unsupported authentication method).
     */
    INVALID_CLIENT("invalid_client"),
    ;

    private final String value;

    OAuth2Error(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

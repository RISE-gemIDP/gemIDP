/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.exception;

import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;

public class AuthorizationException extends RuntimeException {

    private final ErrorMessage errorMessage;
    private final String redirectUri;
    private final String state;

    public AuthorizationException(ErrorMessage errorMessage, String redirectUri, String state) {
        super(errorMessage.getText());
        this.errorMessage = errorMessage;
        this.redirectUri = redirectUri;
        this.state = state;
    }

    public AuthorizationException(ErrorMessage errorMessage, String redirectUri, String state, Throwable cause) {
        super(errorMessage.getText(), cause);
        this.errorMessage = errorMessage;
        this.redirectUri = redirectUri;
        this.state = state;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getState() {
        return state;
    }
}

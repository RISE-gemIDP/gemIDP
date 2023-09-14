/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.exception;

import java.io.Serializable;

/**
 * This class encapsulates a defined accesskeeper error
 */
public class ErrorMessage implements Serializable {

    /** gematik error code */
    private int gematikCode;

    /** oath2 error code */
    private OAuth2Error oAuth2Error;

    /** error description */
    private String text;

    /** _default_ http error code for this error message */
    private int httpError;

    public ErrorMessage(ErrorMessage src, int httpError) {
        this.gematikCode = src.gematikCode;
        this.oAuth2Error = src.oAuth2Error;
        this.text = src.text;
        this.httpError = httpError;
    }

    public ErrorMessage(int gematikCode, String text) {
        this(gematikCode, null, text);
    }

    public ErrorMessage(int gematikCode, OAuth2Error oAuth2Error, String text) {
        this(gematikCode, oAuth2Error, text, 400);
    }

    public ErrorMessage(int gematikCode, OAuth2Error oAuth2Error, String text, int httpError) {
        this.gematikCode = gematikCode;
        this.oAuth2Error = oAuth2Error;
        this.text = text;
        this.httpError = httpError;
    }

    public int getGematikCode() {
        return gematikCode;
    }

    public OAuth2Error getoAuth2Error() {
        return oAuth2Error;
    }

    public String getText() {
        return text;
    }

    public int getHttpError() {
        return httpError;
    }

    @Override
    public String toString() {
        return "ErrorMessage{" +
            "gematikCode=" + gematikCode +
            ", oAuth2Error=" + oAuth2Error +
            ", text='" + text + '\'' +
            ", httpError=" + httpError +
            '}';
    }
}

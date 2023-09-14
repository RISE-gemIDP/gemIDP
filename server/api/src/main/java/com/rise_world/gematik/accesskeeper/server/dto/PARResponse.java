/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PARResponse {

    @JsonProperty("request_uri")
    private String requestUri;
    @JsonProperty("expires_in")
    private Integer expiresIn;

    private String error;

    @JsonProperty("error_description")
    private String errorDescription;


    public PARResponse() {
    }

    public PARResponse(String requestUri, Integer expiresIn) {
        this.requestUri = requestUri;
        this.expiresIn = expiresIn;
    }

    public PARResponse(String requestUri, Integer expiresIn, String error, String errorDescription) {
        this.requestUri = requestUri;
        this.expiresIn = expiresIn;
        this.error = error;
        this.errorDescription = errorDescription;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }
}

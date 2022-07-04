/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UrlBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(UrlBuilder.class);

    private StringBuilder sb = new StringBuilder(500);
    private boolean hasParameters;

    public UrlBuilder(String redirectUri) {
        sb.append(redirectUri);
        hasParameters = StringUtils.contains(redirectUri, '?');
    }

    /**
     * Appends the state parameter
     *
     * @param value the state parameter values
     * @return this instance
     */
    public UrlBuilder appendState(String value) {
        return appendUriParameter("state", value);
    }

    /**
     * Appends a parameter
     *
     * @param name  the parameter name
     * @param value the String parameter value
     * @return this instance
     */
    public UrlBuilder appendParameter(String name, String value) {
        if (value != null) {
            appendParameterName(name);
            sb.append(value);
        }
        return this;
    }

    /**
     * Appends a parameter
     *
     * @param name  the parameter name
     * @param value the int parameter value
     * @return this instance
     */
    public UrlBuilder appendParameter(String name, int value) {
        appendParameterName(name);
        sb.append(value);
        return this;
    }

    /**
     * Appends a parameter
     *
     * @param name  the parameter name
     * @param value the long parameter value
     * @return this instance
     */
    public UrlBuilder appendParameter(String name, long value) {
        appendParameterName(name);
        sb.append(value);
        return this;
    }

    /**
     * Appends a parameter and encodes its value using URL encoding.
     * In case of a {@code null} value, no parameter will be added to the URI.
     *
     * @param name  the parameter name
     * @param value the parameter value (will be encoded)
     * @return this instance
     */
    public UrlBuilder appendUriParameter(String name, String value) {
        if (value == null) {
            return this;
        }

        String encodedValue;
        try {
            encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        }
        catch (UnsupportedEncodingException e) {
            LOG.error("Failed to convert uri", e);
            return this;
        }

        return appendParameter(name, encodedValue);
    }

    private void appendParameterName(String name) {
        if (hasParameters) {
            sb.append('&');
        }
        else {
            sb.append('?');
            hasParameters = true;
        }
        sb.append(name).append('=');
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}

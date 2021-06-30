/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.api.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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
        String encodedState;
        try {
            encodedState = URLEncoder.encode(value, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            LOG.error("Failed to convert state", e);
            return this;
        }

        return appendParameter("state", encodedState);
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

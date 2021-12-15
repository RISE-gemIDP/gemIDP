/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.server.dto.RequestSource;

public class RequestContext {

    private static final ThreadLocal<RequestSource> REQUEST_SOURCE = new ThreadLocal<>();

    private RequestContext() {
        //no instances
    }

    /**
     * Gets the {@link RequestSource} of the current thread.
     *
     * @return the {@link RequestSource}
     */
    public static RequestSource getRequestSource() {
        return REQUEST_SOURCE.get();
    }

    /**
     * Sets the {@link RequestSource} for the current thread.
     *
     * @param requestSource the {@link RequestSource}
     */
    public static void setRequestSource(RequestSource requestSource) {
        REQUEST_SOURCE.set(requestSource);
    }

    /**
     * Remove the current thread's {@link RequestSource}.
     */
    public static void clear() {
        REQUEST_SOURCE.remove();
    }
}

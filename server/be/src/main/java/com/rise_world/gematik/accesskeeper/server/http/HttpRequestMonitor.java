/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.http;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.util.concurrent.Future;

/**
 * {@code HttpRequestMonitor} provides a mechanism to monitor http requests and
 * provides functionality to cancel requests that exceed a specified timeout duration.
 */
public interface HttpRequestMonitor {

    /**
     * {@code monitor} schedules a task to monitor the given HTTP request and cancel the request after the specified timeout duration.
     *
     * @param request {@link HttpUriRequestBase} representing the HTTP request to monitor
     * @return {@link Future cancellation task}, which can be used to control the execution of the cancellation
     */
    Future<Boolean> monitor(HttpUriRequestBase request);
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.util;

import com.rise_world.gematik.accesskeeper.fedmaster.service.Severity;
import com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import static net.logstash.logback.marker.Markers.append;
import static net.logstash.logback.marker.Markers.empty;

/**
 * This class is used to create certificate transparency log entries for federation participants
 */
public class CtrCheckLog {
    private static final String CTR_LOG_NAME = "com.rise_world.gematik.fedmaster.CtrCheckLog";

    private static final Logger CTR_LOG = LoggerFactory.getLogger(CTR_LOG_NAME);

    private CtrCheckLog() {
        // avoid instantiation
    }

    /**
     * Writes a certificate transparency log entry
     *
     * @param statusCode    the status code
     * @param msg           the log message
     * @param params        the message parameters (will be interpolated)
     */
    public static void log(StatusCode statusCode, String msg, Object... params) {
        log(statusCode, empty(), msg, params);
    }

    /**
     * Writes a certificate transparency log entry
     *
     * @param statusCode    the status code
     * @param marker        provided markers to be added to logging
     * @param msg           the log message
     * @param params        the message parameters (will be interpolated)
     */
    public static void log(StatusCode statusCode, Marker marker, String msg, Object... params) {
        if (statusCode.getSeverity() == Severity.ERROR) {
            CTR_LOG.error(append("error_code", statusCode.getCode()).and(marker), msg, params);
        }
        else if (statusCode.getSeverity() == Severity.WARN) {
            CTR_LOG.warn(append("error_code", statusCode.getCode()).and(marker), msg, params);
        }
        else {
            CTR_LOG.info(marker, msg, params);
        }
    }

}

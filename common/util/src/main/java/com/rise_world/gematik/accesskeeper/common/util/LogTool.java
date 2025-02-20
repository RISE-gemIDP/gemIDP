/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * This class is used to create operation and performance log entries
 */
public class LogTool {

    public static final String MDC_REQ_ID = "rqId";
    public static final String MDC_SESSION_ID = "session";
    public static final String MDC_IDP_ISS = "idp_iss";
    public static final String MDC_TOKEN_ENDPOINT = "token_endpoint";
    public static final String MDC_PAR_ENDPOINT = "par_endpoint";
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";
    public static final String MDC_PARENT_SPAN_ID = "parentSpanId";

    public static final String OP_LOG_NAME = "com.rise_world.gematik.accesskeeper.server.OperationLog";
    private static final Logger OP_LOG = LoggerFactory.getLogger(OP_LOG_NAME);

    private LogTool() {
        // avoid instantiation
    }

    /**
     * Writes an operation log entry
     *
     * @param logSeverity the severity
     * @param msg         the log message
     */
    public static void logOperation(LogSeverity logSeverity, String msg) {
        if (logSeverity == LogSeverity.ERROR) {
            OP_LOG.error(msg);
        }
        else {
            OP_LOG.info(msg);
        }
    }

    /**
     * Writes an operation log entry
     *
     * @param logSeverity the severity
     * @param msg         the log message
     * @param params      the message parameters (will be interpolated)
     */
    public static void logOperation(LogSeverity logSeverity, String msg, Object... params) {
        if (logSeverity == LogSeverity.ERROR) {
            OP_LOG.error(msg, params);
        }
        else {
            OP_LOG.info(msg, params);
        }
    }

    /**
     * Writes an operation log (error-)entry
     *
     * @param logSeverity the severity
     * @param msg         the log message
     * @param t           the caught throwable
     */
    public static void logOperation(LogSeverity logSeverity, String msg, Throwable t) {
        if (logSeverity == LogSeverity.ERROR) {
            OP_LOG.error(msg, t);
        }
        else {
            OP_LOG.info(msg, t);
        }
    }

    /**
     * Writes the authentication session id ('snc') to the MDC
     *
     * @param sessionId the sessionId
     */
    public static void setSessionId(String sessionId) {
        MDC.put(MDC_SESSION_ID, sessionId);
    }

    /**
     * Writes the issuer of a remote idp to the MDC
     *
     * @param idpIss the issuer
     */
    public static void setIdpIss(String idpIss) {
        MDC.put(MDC_IDP_ISS, idpIss);
    }

    /**
     * Clear any issuer of a remote idp from the MDC
     */
    public static void clearIdpIss() {
        MDC.remove(MDC_IDP_ISS);
    }

    /**
     * Writes the URI of the remote token endpoint to the MDC
     *
     * @param tokenEndpoint the token endpoint URI
     */
    public static void setTokenEndpoint(String tokenEndpoint) {
        MDC.put(MDC_TOKEN_ENDPOINT, tokenEndpoint);
    }

    /**
     * Writes the URI of the remote pushed auth endpoint to the MDC
     *
     * @param parEndpoint the PAR endpoint URI
     */
    public static void setPAREndpoint(String parEndpoint) {
        MDC.put(MDC_PAR_ENDPOINT, parEndpoint);
    }

    /**
     * Writes a traceId to the MDC. If no traceId is provided a new one is created.
     * A newly created traceId fulfills the needs of a traceId as defined by OpenTracing.
     *
     * @param traceId participating traceId
     */
    public static void startTrace(String traceId) {
        if (StringUtils.isNotEmpty(traceId)) {
            MDC.put(MDC_TRACE_ID, traceId);
        }
        else {
            MDC.put(MDC_TRACE_ID, RandomUtils.randomHex(8));
        }
    }

    /**
     * Writes a newly created spanId to the MDC.
     * If a parent spanId is provided, this spanId will be written as well to the MDC.
     *
     * @param parentSpan parent spanId
     */
    public static void startSpan(String parentSpan) {
        if (StringUtils.isNotEmpty(parentSpan)) {
            MDC.put(MDC_PARENT_SPAN_ID, parentSpan);
        }
        MDC.put(MDC_SPAN_ID, RandomUtils.randomHex(8));
    }

    /**
     * Clears all tracing information from the MDC including all provided baggage names.
     *
     * @param baggage names of all baggage tags to be removed
     */
    public static void clearTracingInformation(String... baggage) {
        // remove standard tracing context
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_SPAN_ID);
        MDC.remove(MDC_PARENT_SPAN_ID);

        // remove tracing baggage
        for (String elem : baggage) {
            MDC.remove(elem);
        }
    }

    public enum LogSeverity {
        INFO,
        ERROR;
    }
}

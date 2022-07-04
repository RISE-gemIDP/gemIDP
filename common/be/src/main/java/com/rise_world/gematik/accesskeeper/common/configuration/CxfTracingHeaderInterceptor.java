/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.configuration;

import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class CxfTracingHeaderInterceptor extends AbstractOutDatabindingInterceptor {

    private String[] propagationIds;
    private String traceId;
    private String spanId;

    public CxfTracingHeaderInterceptor(@Value("${tracing.correlation.trace.id:traceId}")String traceId,
                                       @Value("${tracing.correlation.span.id:spanId}")String spanId,
                                       @Value("${tracing.propagation.keys:}")String... propagationIds) {
        super(Phase.MARSHAL);
        this.propagationIds = propagationIds;
        this.traceId = traceId;
        this.spanId = spanId;
    }

    @Override
    public void handleMessage(Message message) {
        @SuppressWarnings("unchecked")
        Map<String, List<String>> headers = (Map<String, List<String>>) message.computeIfAbsent(Message.PROTOCOL_HEADERS, k -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));

        // modify headers
        headers.put(this.traceId, Collections.singletonList(MDC.get(LogTool.MDC_TRACE_ID)));
        headers.put(this.spanId, Collections.singletonList(MDC.get(LogTool.MDC_SPAN_ID)));

        for (String id : propagationIds) {
            String mdcValue = MDC.get(id);
            if (mdcValue != null) {
                headers.put(id, Collections.singletonList(mdcValue));
            }
        }
    }
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.filter;

import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * Request filter for initializing tracing information in MDC.
 * <p>
 * If a traceId (as configured in {@code tracing.correlation.trace.id}) and/or a spanId (as configured in
 * {@code tracing.correlation.span.id}) is provided in the original request. These information is used to
 * join the existing trace and/or use the provided span information as parent spanId.
 * <p>
 * {@code tracing.correlation.keys} are added as baggage information to the MDC and can be logged as
 * needed.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class TracingFilter implements Filter {

    private String traceId;
    private String spanId;
    private String[] correlationIds;

    public TracingFilter(@Value("${tracing.correlation.trace.id:traceId}")String traceId,
                         @Value("${tracing.correlation.span.id:spanId}")String spanId,
                         @Value("${tracing.correlation.keys:}") String... correlationIds) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.correlationIds = correlationIds;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            LogTool.startTrace(httpServletRequest.getHeader(this.traceId));
            LogTool.startSpan(httpServletRequest.getHeader(this.spanId));

            for (String id : correlationIds) {
                String mdcValue = httpServletRequest.getHeader(id);
                if (mdcValue != null) {
                    MDC.put(id, mdcValue);
                }
            }

            chain.doFilter(request, response);
        }
        finally {
            LogTool.clearTracingInformation(correlationIds);
        }
    }

}

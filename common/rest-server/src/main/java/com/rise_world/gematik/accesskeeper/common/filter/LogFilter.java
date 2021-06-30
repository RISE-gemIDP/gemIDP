/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.filter;

import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Order(1)
@Component
public class LogFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(LogFilter.class);
    private static final String START_MSG = "request start {} url={} remote={}";
    private static final String DONE_MSG = "request done {} url={} code={} time={}ms";

    private Set<String> ignorePaths;

    public LogFilter(@Value("${logFilter.ignoreUris}") String... ignoreUris) {
        LOG.info("ignore URIs: {}", Arrays.toString(ignoreUris));
        ignorePaths = new HashSet<>(Arrays.asList(ignoreUris));
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // no initialization required
    }

    @Override
    @SuppressWarnings("squid:S1181") // we rethrow all Throwables after catching them
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException {
        String remoteAddress = null;

        boolean logRequest = true;
        long start = 0;
        Throwable throwable = null;
        HttpServletRequest req = null;
        HttpServletResponse res = null;
        try {
            req = (HttpServletRequest) request;
            res = (HttpServletResponse) response;

            // set fixed to UTF-8
            req.setCharacterEncoding("UTF-8");
            res.setCharacterEncoding("UTF-8");

            MDC.put(LogTool.MDC_REQ_ID, UUID.randomUUID().toString());

            logRequest = !ignorePaths.contains(req.getRequestURI());
            if (logRequest) {
                LOG.info(START_MSG, req.getMethod(), req.getRequestURI(), remoteAddress);
            }
            start = System.nanoTime();
            req.getParameterMap(); // populate map

            chain.doFilter(request, response);
        }
        catch (Throwable t) {
            throwable = t;
            ExceptionUtils.rethrow(t);
        }
        finally {
            if (logRequest) {
                long time = System.nanoTime() - start;
                int statusCode = res.getStatus();
                String timeMs = formatNanos(time);
                if (throwable == null && statusCode < HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                    LOG.info(DONE_MSG, req.getMethod(), req.getRequestURI(), statusCode, timeMs);
                }
                else {
                    // Note: slf4j can deal with throwable == null
                    LOG.error(DONE_MSG, req.getMethod(), req.getRequestURI(), statusCode, timeMs, throwable);
                }
            }

            MDC.clear();
        }
    }

    private String formatNanos(long nanos) {
        // show millis with 3 digits (i.e. us)
        return new DecimalFormat("#.###", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(nanos / 1000000.0);
    }

    @Override
    public void destroy() {
        LOG.info("destroy");
    }
}

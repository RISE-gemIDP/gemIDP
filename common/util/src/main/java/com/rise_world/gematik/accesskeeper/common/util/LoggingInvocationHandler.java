/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.util;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class LoggingInvocationHandler implements InvocationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingInvocationHandler.class);

    private static final Set<String> EXCLUDED_METHODS;

    static {
        EXCLUDED_METHODS = new HashSet<>();
        for (Method method : Object.class.getDeclaredMethods()) {
            EXCLUDED_METHODS.add(method.getName());
        }
    }

    private String systemName;
    private Object delegate;

    public LoggingInvocationHandler(String systemName, Object delegate) {
        this.systemName = systemName;
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (EXCLUDED_METHODS.contains(method.getName())) {
            return method.invoke(delegate, args);
        }

        LOG.debug("{} remote call start method={}", systemName, method.getName());
        boolean successful = true;
        StopWatch stopWatch = StopWatch.createStarted();
        try {
            return method.invoke(delegate, args);
        }
        catch (InvocationTargetException ite) {
            successful = false;
            Throwable cause = ite.getCause();
            if (cause != null) {
                throw cause;
            }
            throw ite;
        }
        finally {
            stopWatch.stop();
            if (LOG.isInfoEnabled()) {
                LOG.info("{} remote call done method={} duration={}ms successful={}", systemName, method.getName(), formatNanos(stopWatch.getNanoTime()), successful);
            }
        }
    }

    private String formatNanos(long nanos) {
        return new DecimalFormat("#.###", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(nanos / 1000000.0);
    }

    /**
     * Wraps a REST client and logs the request duration
     *
     * @param system the name of the remote system / component
     * @param clazz the interface class
     * @param delegate the delegate object
     * @param <T> the REST interface type
     * @return the created logging proxy
     */
    @SuppressWarnings("unchecked")
    public static <T> T createLoggingProxy(String system, Class<T> clazz, T delegate) {
        return (T) Proxy.newProxyInstance(
            clazz.getClassLoader(),
            new Class[]{clazz},
            new LoggingInvocationHandler(system, delegate));
    }
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class WebConfiguration {

    @Bean
    @Profile("log-requests")
    public CommonsRequestLoggingFilter requestLoggingFilter(@Value("${http.logging.includePayload:true}") boolean includePayload,
                                                            @Value("${http.logging.includeHeaders:true}") boolean includeHeaders,
                                                            @Value("${http.logging.maxPayloadLength:2000}") int maxPayload) {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(includePayload);
        loggingFilter.setMaxPayloadLength(maxPayload);
        loggingFilter.setIncludeHeaders(includeHeaders);
        return loggingFilter;
    }

    @Bean
    @Primary
    // TaskSchedulingAutoConfiguration enabled via @EnableScheduling produces an instance of ThreadPoolTaskScheduler which implements TaskExecutor
    // Therefore we have to define this bean as primary TaskExecutor
    public TaskExecutor getAsyncExecutor(@Value("${asyncExecutor.corePoolSize:1}") int corePoolSize,
                                         @Value("${asyncExecutor.maxPoolSize:2147483647}") int maxPoolSize,
                                         @Value("${asyncExecutor.queueCapacity:20}") int queueCapacity) {
        ThreadPoolTaskExecutor poolExecutor = new ThreadPoolTaskExecutor();
        poolExecutor.setCorePoolSize(corePoolSize);
        poolExecutor.setMaxPoolSize(maxPoolSize);
        poolExecutor.setQueueCapacity(queueCapacity);

        poolExecutor.setTaskDecorator(new ContextCopyingDecorator());
        poolExecutor.initialize();
        return poolExecutor;
    }
}

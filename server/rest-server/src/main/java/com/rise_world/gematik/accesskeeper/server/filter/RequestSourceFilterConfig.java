/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestSourceFilterConfig {

    @Bean
    public FilterRegistrationBean<RequestSourceFilter> loggingFilter() {
        FilterRegistrationBean<RequestSourceFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new RequestSourceFilter());
        registrationBean.addUrlPatterns("/auth/*", "/token", "/.well-known/openid-configuration", "/extauth");
        registrationBean.setOrder(2);

        return registrationBean;
    }
}

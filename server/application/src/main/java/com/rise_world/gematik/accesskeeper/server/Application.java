/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server;

import com.rise_world.gematik.accesskeeper.common.util.BCProviderInit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot Application runner.
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.rise_world.gematik.accesskeeper"})
public class Application extends SpringBootServletInitializer {

    /**
     * Starts the application
     *
     * @param args additional program argument
     */
    //Justification: stream is closed on shutdown via the shutdown hook.
    @SuppressWarnings("squid:S2095")
    public static void main(String[] args) {
        BCProviderInit.init();
        ConfigurableApplicationContext context = SpringApplication.run(Application.class);
        context.registerShutdownHook();
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Application.class);
    }

}

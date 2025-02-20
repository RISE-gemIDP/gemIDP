/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.http;

import com.rise_world.gematik.accesskeeper.server.service.SelfSignedCertificateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class AccessKeeperHttpClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(AccessKeeperHttpClientFactory.class);

    private final SelfSignedCertificateService selfSignedCertificateService;
    private final ScheduledExecutorService executorService;

    AccessKeeperHttpClientFactory(SelfSignedCertificateService selfSignedCertificateService,
                                  @Value("${http-client.monitor.pool-size:5}") int poolSize) {
        this.selfSignedCertificateService = selfSignedCertificateService;
        LOG.info("creating http monitor pool with size {}", poolSize);
        this.executorService = Executors.newScheduledThreadPool(poolSize);
    }

    /**
     * {@code create} creates a {@link AccessKeeperHttpClient} configured using the given {@link AccessKeeperHttpClientConfig config}.
     *
     * @param config {@link AccessKeeperHttpClientConfig}
     * @return configured {@link AccessKeeperHttpClient}
     */
    public AccessKeeperHttpClient create(AccessKeeperHttpClientConfig config) {
        return new AccessKeeperHttpClient(selfSignedCertificateService,
            request -> executorService.schedule(request::cancel, config.requestTimeout().toMillis(), TimeUnit.MILLISECONDS),
            config);
    }

}

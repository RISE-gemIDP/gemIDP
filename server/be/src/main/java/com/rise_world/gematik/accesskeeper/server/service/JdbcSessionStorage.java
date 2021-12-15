/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.common.util.RandomUtils;
import com.rise_world.gematik.accesskeeper.server.entity.ExtSessionEntity;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JdbcSessionStorage implements SessionStorage {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcSessionStorage.class);

    private ExtSessionRepository repo;

    @Autowired
    public JdbcSessionStorage(ExtSessionRepository repo, MeterRegistry meterRegistry, @Value("${db.monitoring.enabled:true}") boolean dbMonitoringEnabled) {
        this.repo = repo;

        LOG.info("JdbcSessionStorage was initialized");

        if (dbMonitoringEnabled) {
            Gauge.builder("db.table.size", repo,
                c -> repo.getSessionCount())
                .tags("table", "extsession")
                .description("The number of external sessions in the database")
                .register(meterRegistry);

            LOG.info("db.table.size gauge was registered");
        }
    }

    @Override
    public String createSessionId() {
        return RandomUtils.randomShortUUID();
    }

    @Override
    public void writeSession(ExtSessionEntity session) {
        repo.save(session);
    }

    @Override
    public ExtSessionEntity getSession(String sessionId) {
        return repo.fetchSession(sessionId).orElse(null);
    }

    @Override
    public void destroySession(String sessionId) {
        repo.deleteSession(sessionId);
    }
}

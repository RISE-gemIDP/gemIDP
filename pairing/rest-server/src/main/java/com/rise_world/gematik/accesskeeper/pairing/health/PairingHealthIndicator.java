/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairing.health;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.ResultSetMetaData;
import java.util.List;

/**
 * Health indicator to check if the pairing database is synced within the cluster and therefore writable.
 */
@Component
public class PairingHealthIndicator extends AbstractHealthIndicator implements InitializingBean {
    public static final String UNKNOWN = "unknown";
    private final DataSource dataSource;
    private final String query;
    private final JdbcTemplate jdbcTemplate;
    private final String expectedValue;

    @Autowired
    public PairingHealthIndicator(DataSource dataSource,
                                  @Value("${management.health.pairing.query}") String query,
                                  @Value("${management.health.pairing.expectedValue}") String expectedValue) {
        super("DataSource health check failed");
        this.dataSource = dataSource;
        this.query = query;
        this.expectedValue = expectedValue;
        this.jdbcTemplate = dataSource != null ? new JdbcTemplate(dataSource) : null;
    }

    /**
     * Validate that datasource is set.
     */
    public void afterPropertiesSet() {
        Assert.state(this.dataSource != null, "DataSource for PairingWritableHealthIndicator must be specified");
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        String validationQuery = this.query;

        builder.withDetail("validationQuery", validationQuery);
        builder.withDetail("expectedResult", expectedValue);

        @SuppressWarnings("findsecbugs:SQL_INJECTION_SPRING_JDBC") // query is statically configured
        List<Object> results = this.jdbcTemplate.query(validationQuery, (rs, rowNum) -> {
            ResultSetMetaData metaData = rs.getMetaData();
            int columns = metaData.getColumnCount();
            if (columns != 1) {
                return UNKNOWN;
            }
            else {
                return rs.getString(1);
            }
        });
        Object result = DataAccessUtils.uniqueResult(results);

        if (result == null) {
            builder.withDetail("pairing-writable", UNKNOWN);
        }
        else {
            builder.withDetail("pairing-writable", expectedValue.equals(result));
        }
        // the service is up even though the database is not writable
        builder.up();
    }

}

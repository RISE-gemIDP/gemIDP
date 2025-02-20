/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.repository;

import com.rise_world.gematik.accesskeeper.fedmaster.schedule.CTRProvider;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;

@Repository
public class CtrCheckLogRepositoryImpl extends JdbcRepository implements CtrCheckLogRepository {

    protected static final String UPSERT = """
        insert into ctr_check_log (id, created_by, modified_by, last_success)
        values
        (:id, :createdBy, :modifiedBy, :lastSuccess)
        on duplicate key
        update
         modified_at = current_timestamp,
         modified_by = :modifiedBy,
         last_success  = :lastSuccess
        """;

    protected static final String LAST_SUCCESS_BY_PROVIDER = "select last_success from ctr_check_log where id = :provider";

    public CtrCheckLogRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public void saveSuccess(CTRProvider provider, Instant timestamp) {
        jdbcTemplate.update(UPSERT,
            Map.ofEntries(entry("id", provider.getId()),
                entry("createdBy", MODIFICATION_VALUE),
                entry("modifiedBy", MODIFICATION_VALUE),
                entry("lastSuccess", Timestamp.from(timestamp))));
    }

    @Override
    public Optional<Instant> lastSuccess(CTRProvider ctrProvider) {
        try {
            return Optional.ofNullable(
                    jdbcTemplate.queryForObject(LAST_SUCCESS_BY_PROVIDER,
                        Map.of("provider", ctrProvider.getId()),
                        Timestamp.class))
                .map(Timestamp::toInstant);
        }
        catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}

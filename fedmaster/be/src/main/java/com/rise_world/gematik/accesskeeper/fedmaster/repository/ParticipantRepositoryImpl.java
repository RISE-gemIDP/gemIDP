/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.repository;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantType;
import com.rise_world.gematik.accesskeeper.fedmaster.schedule.CTRProvider;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantType.OP;
import static java.lang.Boolean.TRUE;
import static java.util.Map.entry;

@Repository
public class ParticipantRepositoryImpl extends JdbcRepository implements ParticipantRepository {

    protected static final String COL_SUBJECT = "subject";
    protected static final String COL_PARTICIPANT_TYPE = "participant_type";
    protected static final String COL_ACTIVE = "active";
    protected static final String COL_ORGANIZATION_NAME = "organization_name";
    protected static final String COL_CLIENT_NAME = "client_name";
    protected static final String COL_LOGO_URI = "logo_uri";
    protected static final String COL_USER_TYPE_SUPPORTED = "user_type_supported";
    protected static final String COL_SYNC_AT = "synchronized_at";
    protected static final String COL_MONITORED_AT = "last_monitored_at";
    protected static final String COL_LAST_RUN = "last_scheduled_run";
    protected static final String COL_ZIS_GROUP = "zis_assignment_group";
    protected static final String COL_PKV = "pkv";
    protected static final String COL_CTR_PROVIDER = "ctr_provider";

    protected static final String SELECT_ACTIVE = "SELECT subject from participant WHERE active=:active";

    protected static final String SELECT_ACTIVE_BY_ID = "SELECT * FROM participant WHERE id=:id AND active=:active";
    protected static final String SELECT_BY_ID = "select * from participant where id = :id";

    protected static final String SELECT_BY_ACTIVE_OP = "SELECT * FROM participant WHERE participant_type=:participant_type AND active=:active";
    protected static final String SELECT_BY_ACTIVE_SUB = "SELECT * FROM participant WHERE subject=:subject AND active=:active";

    protected static final String SELECT_ACTIVE_BY_BEFORE_SYNC_AT = "SELECT * FROM participant" +
        " WHERE " + COL_ACTIVE + "=true AND " + COL_SYNC_AT + "<=:synced";
    protected static final String SELECT_BY_BEFORE_SYNC = "select * from participant p where p.synchronized_at <= :beforeSync";

    protected static final String SELECT_BY_BEFORE_MONITORED_AT = """
        select * from participant p
          left join participant_monitoring_log l on l.participant_id = p.id and l.ctr_provider = :ctr_provider
          where
           p.active is true
           and p.participant_type = :participant_type
           and (l.last_monitored_at <= :last_monitored_at or l.id is null)
        """;

    protected static final String SYNC_PARTICIPANT = "UPDATE participant SET " +
        COL_ORGANIZATION_NAME + "=:org , " +
        COL_LOGO_URI + "=:logo , " +
        COL_MODIFIED_AT + "=CURRENT_TIMESTAMP, " +
        COL_MODIFIED_BY + "=:modified , " +
        COL_SYNC_AT + "=:sync " +
        "WHERE id=:id";
    protected static final String LAST_RUN = "UPDATE participant SET " +
        COL_MODIFIED_AT + "=CURRENT_TIMESTAMP, " +
        COL_MODIFIED_BY + "=:modified , " +
        COL_LAST_RUN + "=:run " +
        "WHERE id=:id";

    protected static final String LAST_MONITORING = """
        insert into participant_monitoring_log (created_by, modified_by, ctr_provider, participant_id, last_monitored_at) values
        (:modified, :modified, :ctr_provider, :participant, :last_monitored_at)
        on duplicate key
        update
          modified_at = current_timestamp,
          modified_by = :modified,
          last_monitored_at = :last_monitored_at
        """;

    protected static final String UPDATE_ACTIVE = """
        update participant set
          modified_at = current_timestamp,
          modified_by = :modified_by,
          active = :active
         where
          id = :id
          and active = :previous_state
        """;
    public ParticipantRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public List<String> findAllIdentifiers() {
        return this.jdbcTemplate.query(
            SELECT_ACTIVE,
            new MapSqlParameterSource(COL_ACTIVE, TRUE),
            SubjectRowMapper.INSTANCE);
    }

    @Override
    public Optional<ParticipantDto> findById(Long id) {
        try {
            Map<String, Object> parameters = new HashMap<>();

            parameters.put(COL_ID, id);
            parameters.put(COL_ACTIVE, TRUE);
            ParticipantDto participant = this.jdbcTemplate.queryForObject(
                SELECT_ACTIVE_BY_ID,
                parameters,
                ParticipantRowMapper.INSTANCE);
            return Optional.ofNullable(participant);
        }
        catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ParticipantDto> findByIdWithInactive(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(SELECT_BY_ID, Map.of(COL_ID, id), ParticipantRowMapper.INSTANCE));
        }
        catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ParticipantDto> findByIdentifier(String identifier) {
        try {
            Map<String, Object> parameters = new HashMap<>();

            parameters.put(COL_SUBJECT, identifier);
            parameters.put(COL_ACTIVE, TRUE);
            ParticipantDto participant = this.jdbcTemplate.queryForObject(
                SELECT_BY_ACTIVE_SUB,
                parameters,
                ParticipantRowMapper.INSTANCE);
            return Optional.ofNullable(participant);
        }
        catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<ParticipantDto> findAllOpenIdProviders() {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put(COL_PARTICIPANT_TYPE, OP.getType());
        parameters.put(COL_ACTIVE, TRUE);
        return this.jdbcTemplate.query(SELECT_BY_ACTIVE_OP, parameters, ParticipantRowMapper.INSTANCE);
    }

    @Override
    public List<ParticipantDto> findBeforeSyncAt(Date syncedBefore) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("synced", syncedBefore);
        return this.jdbcTemplate.query(
            SELECT_ACTIVE_BY_BEFORE_SYNC_AT,
            parameters,
            ParticipantRowMapper.INSTANCE);
    }

    @Override
    public List<ParticipantDto> findBeforeSyncAtWithInactive(Date beforeSync) {
        return jdbcTemplate.query(SELECT_BY_BEFORE_SYNC, Map.of("beforeSync", beforeSync), ParticipantRowMapper.INSTANCE);
    }

    @Override
    public void synchronizeParticipant(ParticipantDto participant, Timestamp sync) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("modified", MODIFICATION_VALUE);
        parameters.put("org", participant.getOrganizationName());
        parameters.put("logo", participant.getLogoUri());
        parameters.put("id", participant.getId());
        parameters.put("sync", sync);

        this.jdbcTemplate.update(
            SYNC_PARTICIPANT,
            parameters);
    }

    @Override
    public void setLastRun(Long identifier, Timestamp run) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("modified", MODIFICATION_VALUE);
        parameters.put("id", identifier);
        parameters.put("run", run);

        this.jdbcTemplate.update(LAST_RUN, parameters);
    }

    @Override
    public void setMonitoringRun(Long identifier, Timestamp monitored, CTRProvider provider) {

        this.jdbcTemplate.update(LAST_MONITORING,
            Map.ofEntries(
                entry("modified", MODIFICATION_VALUE),
                entry(COL_CTR_PROVIDER, provider.getId()),
                entry("participant", identifier),
                entry(COL_MONITORED_AT, monitored)));
    }

    @Override
    public List<ParticipantDto> findBeforeMonitoredAt(Instant monitoringTime, CTRProvider provider) {
        return jdbcTemplate.query(SELECT_BY_BEFORE_MONITORED_AT,
            Map.ofEntries(
                entry(COL_MONITORED_AT, Timestamp.from(monitoringTime)),
                entry(COL_PARTICIPANT_TYPE, OP.getType()),
                entry(COL_CTR_PROVIDER, provider.getId())),
            ParticipantRowMapper.INSTANCE);
    }

    @Override
    public void setActive(Long id, boolean active) {
        jdbcTemplate.update(UPDATE_ACTIVE,
            Map.ofEntries(
                entry(COL_MODIFIED_BY, MODIFICATION_VALUE),
                entry(COL_ACTIVE, active),
                entry(COL_ID, id),
                entry("previous_state", !active)
            ));
    }

    private enum SubjectRowMapper implements RowMapper<String> {
        INSTANCE;

        @Override
        public String mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getString(COL_SUBJECT);
        }
    }

    private enum ParticipantRowMapper implements RowMapper<ParticipantDto> {
        INSTANCE;

        @Override
        public ParticipantDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            ParticipantDto entity = new ParticipantDto();
            entity.setId(rs.getLong(COL_ID));
            entity.setSub(rs.getString(COL_SUBJECT));
            entity.setType(ParticipantType.getByType(rs.getString(COL_PARTICIPANT_TYPE)));
            entity.setActive(rs.getBoolean(COL_ACTIVE));
            entity.setOrganizationName(rs.getString(COL_ORGANIZATION_NAME));
            entity.setClientName(rs.getString(COL_CLIENT_NAME));
            entity.setLogoUri(rs.getString(COL_LOGO_URI));
            entity.setUserTypeSupported(rs.getString(COL_USER_TYPE_SUPPORTED));
            entity.setSynchronizedAt(rs.getTimestamp(COL_SYNC_AT));
            entity.setLastScheduledRun(rs.getTimestamp(COL_LAST_RUN));
            entity.setZisGroup(rs.getString(COL_ZIS_GROUP));
            entity.setPkv(rs.getBoolean(COL_PKV));

            return entity;
        }
    }

}

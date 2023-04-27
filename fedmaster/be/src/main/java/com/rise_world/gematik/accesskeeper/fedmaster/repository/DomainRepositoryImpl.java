/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.repository;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDomainDto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DomainRepositoryImpl extends JdbcRepository implements DomainRepository {
    private static final String COL_NAME = "domain_name";
    private static final String COL_PARTICIPANT_ID = "participant_id";

    private static final String SELECT_BY_PARTICIPANT = "SELECT * FROM domain where participant_id=:participant_id";
    private static final String DELETE_BY_ID = "DELETE FROM domain WHERE id=:id";
    private static final String INSERT = "INSERT INTO domain (created_by, modified_by, domain_name, participant_id) VALUES (:created, :modified, :domain_name, :participant_id)";

    public DomainRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public List<ParticipantDomainDto> findByParticipant(Long identifier) {
        return this.jdbcTemplate.query(
            SELECT_BY_PARTICIPANT,
            new MapSqlParameterSource(COL_PARTICIPANT_ID, identifier),
            ParticipantDomainRowMapper.INSTANCE
        );
    }

    @Override
    public void delete(Long identifier) {
        this.jdbcTemplate.update(
            DELETE_BY_ID,
            new MapSqlParameterSource(COL_ID, identifier)
        );
    }

    @Override
    public void save(ParticipantDomainDto domain) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("created", MODIFICATION_VALUE);
        parameters.put("modified", MODIFICATION_VALUE);
        parameters.put(COL_NAME, domain.getName());
        parameters.put(COL_PARTICIPANT_ID, domain.getParticipantId());
        this.jdbcTemplate.update(
            INSERT,
            parameters
        );
    }

    protected enum ParticipantDomainRowMapper implements RowMapper<ParticipantDomainDto> {
        INSTANCE;

        @Override
        public ParticipantDomainDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            ParticipantDomainDto entity = new ParticipantDomainDto();
            entity.setId(rs.getLong(COL_ID));
            entity.setName(rs.getString(COL_NAME));
            entity.setParticipantId(rs.getLong(COL_PARTICIPANT_ID));
            return entity;
        }
    }
}

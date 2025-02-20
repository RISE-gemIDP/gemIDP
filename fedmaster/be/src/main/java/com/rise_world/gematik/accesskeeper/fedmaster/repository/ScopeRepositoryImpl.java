/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.repository;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantScopes;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class ScopeRepositoryImpl extends JdbcRepository implements ScopeRepository {

    private static final String COL_PARTICIPANT_ID = "participant_id";

    private static final String SELECT_BY_PARTICIPANT = "SELECT scope_name FROM scope WHERE participant_id=:participant_id";

    public ScopeRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public ParticipantScopes findByParticipant(Long identifier) {
        return new ParticipantScopes(this.jdbcTemplate.queryForList(
            SELECT_BY_PARTICIPANT,
            Map.of(COL_PARTICIPANT_ID, identifier),
            String.class));
    }
}

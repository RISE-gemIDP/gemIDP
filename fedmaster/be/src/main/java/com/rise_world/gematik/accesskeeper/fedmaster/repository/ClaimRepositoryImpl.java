/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.repository;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantClaims;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class ClaimRepositoryImpl extends JdbcRepository implements ClaimRepository {

    private static final String COL_PARTICIPANT_ID = "participant_id";

    private static final String SELECT_BY_PARTICIPANT = "SELECT claim_name FROM claim WHERE participant_id=:participant_id";

    public ClaimRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public ParticipantClaims findByParticipant(Long identifier) {
        return new ParticipantClaims(this.jdbcTemplate.queryForList(
            SELECT_BY_PARTICIPANT,
            Map.of(COL_PARTICIPANT_ID, identifier),
            String.class));
    }
}

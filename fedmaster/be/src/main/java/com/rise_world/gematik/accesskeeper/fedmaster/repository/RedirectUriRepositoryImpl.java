/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.repository;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantRedirectUris;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class RedirectUriRepositoryImpl extends JdbcRepository implements RedirectUriRepository {

    private static final String COL_PARTICIPANT_ID = "participantId";

    private static final String SELECT_BY_PARTICIPANT = "SELECT uri FROM redirect_uri WHERE participant_id = :participantId";

    public RedirectUriRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public ParticipantRedirectUris findByParticipant(Long identifier) {
        return new ParticipantRedirectUris(jdbcTemplate.queryForList(
            SELECT_BY_PARTICIPANT,
            Map.of(COL_PARTICIPANT_ID, identifier),
            String.class));
    }
}

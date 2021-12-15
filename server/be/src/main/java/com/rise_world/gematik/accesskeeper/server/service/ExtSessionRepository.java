/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.server.entity.ExtSessionEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class ExtSessionRepository {

    private static final String TABLE_NAME = "extsession";
    private static final String COL_STATE = "state";

    private static final String QUERY_SESSION = "SELECT * FROM " + TABLE_NAME + " WHERE state=:state";
    private static final String DELETE_SESSION = "DELETE FROM " + TABLE_NAME + " WHERE state=:state";
    private static final String COUNT_SESSION = "SELECT COUNT(*) FROM " + TABLE_NAME;

    private final SimpleJdbcInsert simpleJdbcInsert;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public ExtSessionRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        simpleJdbcInsert = new SimpleJdbcInsert(namedParameterJdbcTemplate.getJdbcTemplate())
            .withTableName(TABLE_NAME);
        simpleJdbcInsert.compile();

        this.jdbcTemplate = namedParameterJdbcTemplate;
    }

    /**
     * Stores the session.
     *
     * @param entity the external session to be stored
     */
    @Transactional
    public void save(ExtSessionEntity entity) {
        simpleJdbcInsert.execute(new BeanPropertySqlParameterSource(entity));
    }

    /**
     * Loads the external session with the given state from the repository.
     *
     * @param state the state parameter (primary key)
     * @return the external session or {@code Optional.empty()} if no session exists
     */
    // @AFO: A_22265 - Sitzungsdaten werden geladen
    public Optional<ExtSessionEntity> fetchSession(String state) {
        BeanPropertyRowMapper<ExtSessionEntity> mapper = new BeanPropertyRowMapper<>(ExtSessionEntity.class);
        List<ExtSessionEntity> pairingEntryEntities = jdbcTemplate.query(
            QUERY_SESSION,
            new MapSqlParameterSource().addValue(COL_STATE, state),
            mapper
        );

        if (pairingEntryEntities.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(pairingEntryEntities.get(0));
    }

    /**
     * Deletes the session with the given state from the repository.
     *
     * @param state the state parameter (primary key)
     * @return {@code true} if the session was deleted, {@code false} if no session
     * was deleted
     */
    @Transactional
    public boolean deleteSession(String state) {
        int affectedRows = jdbcTemplate.update(DELETE_SESSION,
            new MapSqlParameterSource().addValue(COL_STATE, state));
        return (affectedRows > 0);
    }

    /**
     * Returns the number of active (existing) sessions
     *
     * @return the number of sessions
     */
    public Integer getSessionCount() {
        return jdbcTemplate.queryForObject(COUNT_SESSION, Collections.emptyMap(), Integer.class);
    }
}

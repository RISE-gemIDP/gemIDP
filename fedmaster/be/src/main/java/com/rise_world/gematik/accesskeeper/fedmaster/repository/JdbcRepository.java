/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public abstract class JdbcRepository {

    public static final String COL_ID = "id";
    public static final String COL_CREATED_BY = "created_by";
    public static final String COL_MODIFIED_AT = "modified_at";
    public static final String COL_MODIFIED_BY = "modified_by";
    public static final String MODIFICATION_VALUE = "SYS_FEDMASTER";

    protected final NamedParameterJdbcTemplate jdbcTemplate;

    protected JdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

}


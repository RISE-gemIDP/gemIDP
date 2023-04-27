/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.repository;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.CertificatePublicKeyDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.KeyType;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantKeyDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PublicKeyRepositoryImpl extends JdbcRepository implements PublicKeyRepository {

    private static final String COL_PARTICIPANT_ID = "participant_id";
    private static final String COL_ACTIVE = "active";
    private static final String COL_PEM = "pem";
    private static final String COL_DOMAIN = "domain";
    private static final String COL_KEY_TYPE = "key_type";
    private static final String COL_KID = "kid";
    private static final String SELECT_BY_ACTIVE_PARTICIPANT = "SELECT * FROM public_key WHERE active=:active AND participant_id=:participant_id AND key_type=:key_type";
    private static final String SELECT_BY_ACTIVE_PART_KID = "SELECT * FROM public_key WHERE active=:active AND kid=:kid AND participant_id=:participant_id AND key_type=:key_type";

    public PublicKeyRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public List<CertificatePublicKeyDto> findAllCertificateKeysByParticipant(Long participantId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(COL_ACTIVE, Boolean.TRUE);
        parameters.put(COL_PARTICIPANT_ID, participantId);
        parameters.put(COL_KEY_TYPE, KeyType.CTR.name().toLowerCase());

        return this.jdbcTemplate.query(
            SELECT_BY_ACTIVE_PARTICIPANT,
            new MapSqlParameterSource(parameters),
            CertificatePublicKeyRowMapper.INSTANCE);
    }

    @Override
    public List<ParticipantKeyDto> findByParticipant(Long identifier) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(COL_ACTIVE, Boolean.TRUE);
        parameters.put(COL_KEY_TYPE, KeyType.SIGNATURE.name().toLowerCase());
        parameters.put(COL_PARTICIPANT_ID, identifier);

        return this.jdbcTemplate.query(
            SELECT_BY_ACTIVE_PARTICIPANT,
            new MapSqlParameterSource(parameters),
            ParticipantKeyRowMapper.INSTANCE);
    }

    @Override
    public Optional<ParticipantKeyDto> findKeyByParticipantAndKeyId(Long participantId, String kid) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(COL_ACTIVE, Boolean.TRUE);
        parameters.put(COL_KEY_TYPE, KeyType.SIGNATURE.name().toLowerCase());
        parameters.put(COL_KID, kid);
        parameters.put(COL_PARTICIPANT_ID, participantId);

        try {
            ParticipantKeyDto key = this.jdbcTemplate.queryForObject(
                SELECT_BY_ACTIVE_PART_KID,
                new MapSqlParameterSource(parameters),
                ParticipantKeyRowMapper.INSTANCE
            );
            return Optional.ofNullable(key);
        }
        catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private enum ParticipantKeyRowMapper implements RowMapper<ParticipantKeyDto> {
            INSTANCE;

        @Override
        public ParticipantKeyDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            ParticipantKeyDto entity = new ParticipantKeyDto();
            entity.setPem(rs.getString(COL_PEM));
            entity.setKeyId(rs.getString(COL_KID));
            return entity;
        }
    }

    private enum CertificatePublicKeyRowMapper implements RowMapper<CertificatePublicKeyDto> {
        INSTANCE;

        @Override
        public CertificatePublicKeyDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            CertificatePublicKeyDto entity = new CertificatePublicKeyDto();
            entity.setPem(rs.getString(COL_PEM));
            entity.setDomain(rs.getString(COL_DOMAIN));
            return entity;
        }
    }
}

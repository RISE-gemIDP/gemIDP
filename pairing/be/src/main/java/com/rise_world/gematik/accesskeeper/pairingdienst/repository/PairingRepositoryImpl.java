/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.repository;

import com.rise_world.gematik.accesskeeper.pairingdienst.entity.PairingEntryEntity;
import com.rise_world.gematik.accesskeeper.pairingdienst.exception.PairingDienstException;
import com.rise_world.gematik.accesskeeper.pairingdienst.util.Utils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes.REG4_DUPLICATE_PAIRING;
import static com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes.SERVER_ERROR;
import static com.rise_world.gematik.accesskeeper.pairingdienst.exception.ErrorDetails.DATABASE_INCONSISTENT;

/**
 * Implementation of {@code PairingRepository}.
 */
@Repository
public class PairingRepositoryImpl implements PairingRepository {

    private static final Logger LOG = LoggerFactory.getLogger(PairingRepositoryImpl.class);
    private static final Logger DELETE_LOG = LoggerFactory.getLogger("com.rise_world.gematik.accesskeeper.pairingdienst.repository.DeleteLog");

    private static final String TABLE_NAME = "pairingentry";
    private static final String COL_ID = "id";
    private static final String COL_ID_NUMMER = "idNummer";
    private static final String COL_KEY_IDENTIFIER = "keyIdentifier";

    private static final String QUERY_PAIRING = "SELECT * FROM " + TABLE_NAME + " WHERE idNummer=:idNummer AND keyIdentifier=:keyIdentifier";
    private static final String QUERY_PAIRINGS = "SELECT * FROM " + TABLE_NAME + " WHERE idNummer=:idNummer ORDER BY deviceName, id";
    private static final String DELETE_PAIRING = "DELETE FROM " + TABLE_NAME + " WHERE id=:id";

    private final SimpleJdbcInsert simpleJdbcInsert;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public PairingRepositoryImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        simpleJdbcInsert = new SimpleJdbcInsert(namedParameterJdbcTemplate.getJdbcTemplate())
            .withTableName(TABLE_NAME)
            .usingGeneratedKeyColumns("id");
        simpleJdbcInsert.compile();

        this.jdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    @Transactional
    public long save(PairingEntryEntity pairing) {
        Number id;

        try {
            // convert key identifier to hex value
            final PairingEntryEntity toPersist = PairingEntryEntity.aPairingEntryEntity()
                .withIdNummer(pairing.getIdNummer())
                .withKeyIdentifier(convertBase64ToHex(pairing.getKeyIdentifier()))
                .withSignedPairingData(pairing.getSignedPairingData())
                .withCreationTime(pairing.getCreationTime())
                .withDeviceName(pairing.getDeviceName())
                .withPairingEntryVersion(pairing.getPairingEntryVersion())
                .build();

            id = simpleJdbcInsert.executeAndReturnKey(new BeanPropertySqlParameterSource(toPersist));
        }
        catch (DuplicateKeyException e) {
            // @AFO A_21424 - wenn bereits ein Eintrag mit dem Tupel <idNummer, keyIdentifier> existiert, dann wird mit REG4 geantwortet
            throw new PairingDienstException(REG4_DUPLICATE_PAIRING, e);
        }
        return id.longValue();
    }

    @Override
    public Optional<PairingEntryEntity> fetchPairing(String idNummer, String keyIdentifier) {
        List<PairingEntryEntity> pairingEntryEntities = jdbcTemplate.query(
            QUERY_PAIRING,
            new MapSqlParameterSource().
                addValue(COL_ID_NUMMER, idNummer).
                addValue(COL_KEY_IDENTIFIER, convertBase64ToHex(keyIdentifier)),
            PairingEntryRowMapper.INSTANCE
        );

        if (pairingEntryEntities.size() > 1) {
            LOG.error("lookup returned more than one record");
            throw new PairingDienstException(SERVER_ERROR, DATABASE_INCONSISTENT);
        }

        if (pairingEntryEntities.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(pairingEntryEntities.get(0));
    }

    @Override
    public List<PairingEntryEntity> fetchPairings(String idNummer) {
        return jdbcTemplate.query(QUERY_PAIRINGS, new MapSqlParameterSource().addValue(COL_ID_NUMMER, idNummer),
                PairingEntryRowMapper.INSTANCE);
    }

    @Override
    @Transactional
    public boolean deletePairing(String idNummer, String keyIdentifier) {
        Optional<PairingEntryEntity> pairing = fetchPairing(idNummer, keyIdentifier);

        if (!pairing.isPresent()) {
            return false;
        }

        // @AFO: A_21448 Löschung des Pairing Datensatzes in der Datenbank
        Long pairingId = pairing.get().getId();
        int affectedRows = jdbcTemplate.update(DELETE_PAIRING,
            new MapSqlParameterSource().addValue(COL_ID, pairingId));

        DELETE_LOG.info(DELETE_PAIRING.replace(":id", pairingId.toString()) + ";");

        // return true if pairing was deleted
        return (affectedRows > 0);
    }

    private enum PairingEntryRowMapper implements RowMapper<PairingEntryEntity> {

        INSTANCE;

        @Override
        public PairingEntryEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            return PairingEntryEntity.aPairingEntryEntity()
                .withId(rs.getLong(COL_ID))
                .withIdNummer(rs.getString(COL_ID_NUMMER))
                .withKeyIdentifier(convertHexToBase64(rs.getString(COL_KEY_IDENTIFIER)))
                .withPairingEntryVersion(rs.getString("pairingEntryVersion"))
                .withDeviceName(rs.getString("deviceName"))
                .withCreationTime(rs.getTimestamp("creationTime"))
                .withSignedPairingData(rs.getString("signedPairingData"))
                .build();
        }
    }

    private static String convertBase64ToHex(String base64) {
        return Hex.toHexString(Utils.BASE64URL_DECODER.decode(base64));
    }

    private static String convertHexToBase64(String hex) {
        return Utils.BASE64URL_ENCODER.encodeToString(Hex.decodeStrict(hex));
    }
}

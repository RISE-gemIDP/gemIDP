/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.repository;

import com.rise_world.gematik.accesskeeper.pairingdienst.dto.DeviceStatus;
import com.rise_world.gematik.accesskeeper.pairingdienst.dto.DeviceTypeDTO;
import com.rise_world.gematik.accesskeeper.pairingdienst.exception.PairingDienstException;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes.SERVER_ERROR;
import static com.rise_world.gematik.accesskeeper.pairingdienst.exception.ErrorDetails.DATABASE_INCONSISTENT;

@Repository
public class BlockAllowListRepositoryImpl implements BlockAllowListRepository {

    protected static final String TABLE_NAME = "blockallowlist";
    protected static final String COL_DEVICE_STATE = "deviceState";
    protected static final String QUERY_STATE = "SELECT " + COL_DEVICE_STATE + " FROM " + TABLE_NAME +
        " WHERE manufacturer=:manufacturer AND" +
        " product=:product AND" +
        " model=:model AND" +
        " os=:os AND" +
        " osVersion=:osVersion";

    private static final Logger LOG = LoggerFactory.getLogger(BlockAllowListRepositoryImpl.class);

    private NamedParameterJdbcTemplate jdbcTemplate;

    public BlockAllowListRepositoryImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public DeviceStatus fetchDeviceStatus(DeviceTypeDTO deviceType) {
        List<DeviceStatus> states = this.jdbcTemplate.query(QUERY_STATE,
            new MapSqlParameterSource()
                .addValue("manufacturer", deviceType.getManufacturer())
                .addValue("product", deviceType.getProduct())
                .addValue("model", deviceType.getModel())
                .addValue("os", deviceType.getOs())
                .addValue("osVersion", deviceType.getOsVersion()),
            BlockAllowListRepositoryImpl::extractDeviceState);

        if (states.size() > 1) {
            LOG.error("lookup returned more than one record");
            throw new PairingDienstException(SERVER_ERROR, DATABASE_INCONSISTENT);
        }

        if (states.isEmpty()) {
            return DeviceStatus.UNKNOWN;
        }

        return states.get(0);
    }

    /**
     * RowMapper implementation for DeviceStatus
     *
     * @param rs     The ResultSet that contains the device state column
     * @param rowNum current row number
     * @return state represented in deviceState column of the provided ResultSet
     * @throws SQLException
     */
    @SuppressWarnings("squid:1172") // parameter rowNum needed to fulfil RowMapper interface
    protected static DeviceStatus extractDeviceState(ResultSet rs, int rowNum) throws SQLException {
        String state = StringUtils.upperCase(rs.getString(COL_DEVICE_STATE));
        return EnumUtils.getEnum(DeviceStatus.class, state, DeviceStatus.UNKNOWN);
    }
}

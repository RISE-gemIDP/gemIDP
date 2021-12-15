/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.actuator;

import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import com.rise_world.gematik.accesskeeper.server.service.ConfigService;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * A custom actuator endpoint which allows to query and reload the infomodel
 */
@Component
@Endpoint(id = "infomodel")
public class InfoModelEndpoint {

    private ConfigService configService;

    @Autowired
    public InfoModelEndpoint(ConfigService configService) {
        this.configService = configService;
    }

    @WriteOperation
    public Map<String, String> reloadInfomodel() {
        configService.reload();

        String requestId = MDC.get(LogTool.MDC_REQ_ID);
        Map<String, String> response = new HashMap<>();
        response.put(LogTool.MDC_REQ_ID, requestId);
        return response;
    }

    @ReadOperation
    public Map<String, Object> getActiveConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("config_location", configService.getConfigLocation());
        config.put("effective_info_model", configService.getEffectiveInfoModel());
        config.put("parsed_info_model", configService.getParsedInfoModel());
        config.put("invalid_client_ids", configService.getInvalidClientIds());
        config.put("invalid_fachdienst_ids", configService.getInvalidFachdienstIds());
        config.put("invalid_scope_ids", configService.getInvalidScopeIds());
        config.put("invalid_sektor_app_ids", configService.getInvalidSektorAppIds());
        return config;
    }
}

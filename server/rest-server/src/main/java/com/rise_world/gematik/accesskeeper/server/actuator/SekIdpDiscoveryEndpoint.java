/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.actuator;

import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import com.rise_world.gematik.accesskeeper.server.dto.RemoteIdpDTO;
import com.rise_world.gematik.accesskeeper.server.model.SektorApp;
import com.rise_world.gematik.accesskeeper.server.service.ConfigService;
import com.rise_world.gematik.accesskeeper.server.service.DirectoryService;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * A custom actuator endpoint which allows to query and refresh the remote IDP discovery document cache
 */
@Component
@Endpoint(id = "sekidpdd")
public class SekIdpDiscoveryEndpoint {

    private ConfigService configService;
    private DirectoryService directoryService;

    @Autowired
    public SekIdpDiscoveryEndpoint(ConfigService configService, DirectoryService directoryService) {
        this.configService = configService;
        this.directoryService = directoryService;
    }

    @WriteOperation
    public Map<String, String> refreshCache() {
        directoryService.rebuildDirectoryCache();

        String requestId = MDC.get(LogTool.MDC_REQ_ID);
        Map<String, String> response = new HashMap<>();
        response.put(LogTool.MDC_REQ_ID, requestId);
        return response;
    }

    @ReadOperation
    public Map<String, Object> getCache() {
        Map<String, Object> cache = new HashMap<>();

        for (SektorApp sektorApp : configService.getSektorApps()) {
            RemoteIdpDTO remoteIdpConfig = directoryService.getRemoteIdpConfig(sektorApp.getId());

            if (remoteIdpConfig != null) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("issuer", remoteIdpConfig.getIssuer());
                properties.put("token_endpoint", remoteIdpConfig.getTokenEndpoint());
                properties.put("keys", remoteIdpConfig.getWebKeys().keySet());
                properties.put("created_at", remoteIdpConfig.getCreatedAt());
                cache.put(sektorApp.getId(), properties);
            }
        }
        return cache;
    }
}

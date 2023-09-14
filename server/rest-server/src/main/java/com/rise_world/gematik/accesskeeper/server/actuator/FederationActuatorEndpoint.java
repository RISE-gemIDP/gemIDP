/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.actuator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rise_world.gematik.accesskeeper.common.JwkUtils;
import com.rise_world.gematik.accesskeeper.server.service.EntityStatementSynchronizationService;
import com.rise_world.gematik.idp.server.api.discovery.JsonWebKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Endpoint(id = "fedidps")
public class FederationActuatorEndpoint {

    private final EntityStatementSynchronizationService entityStatementSynchronizationService;
    private final ObjectMapper objectMapper;

    @Autowired
    public FederationActuatorEndpoint(EntityStatementSynchronizationService entityStatementSynchronizationService, ObjectMapper objectMapper) {
        this.entityStatementSynchronizationService = entityStatementSynchronizationService;
        this.objectMapper = objectMapper;
    }

    @WriteOperation
    public List<Map<String, Object>> updateEntityStatementCache() {
        entityStatementSynchronizationService.updateEntityStatementCache();
        return getEntityStatementCache();
    }

    @ReadOperation
    public List<Map<String, Object>> getEntityStatementCache() {
        return entityStatementSynchronizationService.getEntityStatementCache().stream()
            .map(es -> {
                Map<String, Object> map = objectMapper.convertValue(es, Map.class);
                map.put("keys", transform(es.getKeys()));
                return map;
            }).collect(Collectors.toList());
    }

    private Map<String, JsonWebKey> transform(Map<String, org.apache.cxf.rs.security.jose.jwk.JsonWebKey> keys) {
        return keys
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> JwkUtils.transform(entry.getValue())));
    }
}

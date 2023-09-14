/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.api.authserver;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.server.service.AuthServerEntityStatementService;
import com.rise_world.gematik.idp.server.api.federation.FederationConfigurationEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FederationConfigurationEndpointImpl implements FederationConfigurationEndpoint {

    private final AuthServerEntityStatementService service;

    @Autowired
    public FederationConfigurationEndpointImpl(AuthServerEntityStatementService service) {
        this.service = service;
    }

    @Override
    public String getFederationEntity() {
        try {
            // A_23034 die Erstellung des Entitystatements wird an das Service delegiert
            return service.createEntityStatement();
        }
        catch (Exception ex) {
            throw new AccessKeeperException(ErrorCodes.SERVER_ERROR, ex);
        }
    }
}

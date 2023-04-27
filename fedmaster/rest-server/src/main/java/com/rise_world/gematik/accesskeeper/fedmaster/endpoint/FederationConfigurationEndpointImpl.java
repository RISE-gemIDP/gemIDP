/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.endpoint;

import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.fedmaster.exception.FederationMasterException;
import com.rise_world.gematik.accesskeeper.fedmaster.service.EntityStatementService;
import com.rise_world.gematik.idp.server.api.federation.FederationConfigurationEndpoint;
import org.springframework.web.bind.annotation.RestController;

import static com.rise_world.gematik.accesskeeper.fedmaster.exception.FederationMasterOperation.FETCH_MASTER;

@RestController
public class FederationConfigurationEndpointImpl implements FederationConfigurationEndpoint {

    private final EntityStatementService service;

    public FederationConfigurationEndpointImpl(EntityStatementService service) {
        this.service = service;
    }

    @Override
    public String getFederationEntity() {
        try {
            return service.fetchMasterEntityStatement();
        }
        catch (Exception ex) {
            throw new FederationMasterException(ErrorCodes.SERVER_ERROR, FETCH_MASTER, ex);
        }
    }
}

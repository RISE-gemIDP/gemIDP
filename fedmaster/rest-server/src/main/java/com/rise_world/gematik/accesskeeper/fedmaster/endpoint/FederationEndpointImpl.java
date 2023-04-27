/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.endpoint;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.fedmaster.exception.FederationMasterException;
import com.rise_world.gematik.accesskeeper.fedmaster.service.EntityStatementService;
import com.rise_world.gematik.idp.server.api.federation.FederationEndpoint;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes.SERVER_ERROR;
import static com.rise_world.gematik.accesskeeper.fedmaster.exception.FederationMasterOperation.FETCH;
import static com.rise_world.gematik.accesskeeper.fedmaster.exception.FederationMasterOperation.IDP_LIST;
import static com.rise_world.gematik.accesskeeper.fedmaster.exception.FederationMasterOperation.LIST;

@RestController
public class FederationEndpointImpl implements FederationEndpoint {

    private final EntityStatementService service;

    public FederationEndpointImpl(EntityStatementService service) {
        this.service = service;
    }

    @Override
    public String fetchEndpoint(String iss, String sub, String aud) {
        try {
            return service.fetchEntityStatement(iss, sub, aud);
        }
        catch (AccessKeeperException ex) {
            throw new FederationMasterException(ex.getErrorMessage(), FETCH, ex);
        }
        catch (Exception ex) {
            throw new FederationMasterException(SERVER_ERROR, FETCH, ex);
        }
    }

    @Override
    public List<String> listEntities() {
        try {
            return service.getSubEntityIds();
        }
        catch (Exception ex) {
            throw new FederationMasterException(SERVER_ERROR, LIST, ex);
        }
    }

    @Override
    public String listIdentityProviders() {
        try {
            return service.getIdpList();
        }
        catch (Exception ex) {
            throw new FederationMasterException(SERVER_ERROR, IDP_LIST, ex);
        }
    }
}

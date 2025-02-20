/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.api.discovery;

import com.rise_world.gematik.accesskeeper.server.service.IdentityFederationDirectoryService;
import com.rise_world.gematik.idp.server.api.discovery.DirectoryEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DirectoryEndpointImpl implements DirectoryEndpoint {

    private final IdentityFederationDirectoryService identityFederationDirectoryService;

    @Autowired
    public DirectoryEndpointImpl(IdentityFederationDirectoryService identityFederationDirectoryService) {
        this.identityFederationDirectoryService = identityFederationDirectoryService;
    }

    @Override
    public String getFederatedIDPs() {
        return identityFederationDirectoryService.getRemoteIdps();
    }
}

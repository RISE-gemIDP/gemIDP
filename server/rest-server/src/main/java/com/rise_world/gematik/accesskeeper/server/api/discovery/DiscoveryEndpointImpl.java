/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.api.discovery;

import com.rise_world.gematik.accesskeeper.server.service.DiscoveryService;
import com.rise_world.gematik.idp.server.api.discovery.DiscoveryEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

// @AFO: A_20686 - separates Interface f&uuml;r DISC
@RestController
public class DiscoveryEndpointImpl implements DiscoveryEndpoint {

    private final DiscoveryService discoveryService;

    @Autowired
    public DiscoveryEndpointImpl(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @Override
    public String getDiscoveryDocument() {
        return discoveryService.getDiscoverDocument();
    }
}

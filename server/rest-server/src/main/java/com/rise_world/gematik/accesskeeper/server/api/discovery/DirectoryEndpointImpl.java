/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.api.discovery;

import com.rise_world.gematik.accesskeeper.server.service.DirectoryService;
import com.rise_world.gematik.idp.server.api.discovery.DirectoryEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DirectoryEndpointImpl implements DirectoryEndpoint {

    private DirectoryService directory;

    @Autowired
    public DirectoryEndpointImpl(DirectoryService directory) {
        this.directory = directory;
    }

    @Override
    public String getAllKkApps() {
        // A_22284: Die Erstellung der Liste wird an das Service delegiert
        return directory.getAppDirectory();
    }
}

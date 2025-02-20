/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "federation.ctr.fedmaster")
public class FederationMasterDomainCheckConfiguration {

    private List<String> publicKeys = List.of();
    private String zisGroup;

    public List<String> getPublicKeys() {
        return List.copyOf(publicKeys);
    }

    public void setPublicKeys(List<String> publicKeys) {
        this.publicKeys = publicKeys;
    }

    public String getZisGroup() {
        return zisGroup;
    }

    public void setZisGroup(String zisGroup) {
        this.zisGroup = zisGroup;
    }
}

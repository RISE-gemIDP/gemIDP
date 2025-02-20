/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.rise_world.gematik.accesskeeper.common.util.LoggingInvocationHandler;
import com.rise_world.gematik.accesskeeper.crtsh.CrtShMonitor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.rise_world.gematik.accesskeeper.fedmaster.FederationMasterConfiguration.USER_AGENT;

@Service
public class CrtShMonitorFactory {

    private final JacksonJsonProvider jsonProvider;
    private final CrtShConfiguration configuration;

    public CrtShMonitorFactory(JacksonJsonProvider jsonProvider, CrtShConfiguration configuration) {
        this.jsonProvider = jsonProvider;
        this.configuration = configuration;
    }

    /**
     * {@code createMonitor} creates a {@link CrtShMonitor crt.sh client} respecting {@link CrtShConfiguration}
     * @return a {@link CrtShMonitor crt.sh client}
     */
    public CrtShMonitor createMonitor() {
        var client = JAXRSClientFactory.create(configuration.getEndpoint(), CrtShMonitor.class, List.of(jsonProvider));

        var config = WebClient.getConfig(client);
        config.getResponseContext().put("buffer.proxy.response", Boolean.TRUE); // GEMIDP-1244 prevent connection leaks

        var httpClientPolicy = config.getHttpConduit().getClient();
        httpClientPolicy.setBrowserType(USER_AGENT);

        httpClientPolicy.setConnectionTimeout(configuration.getConnectionTimeout().toMillis());
        httpClientPolicy.setReceiveTimeout(configuration.getReceiveTimeout().toMillis());

        return LoggingInvocationHandler.createLoggingProxy("CtrShSearch", CrtShMonitor.class, client);
    }
}

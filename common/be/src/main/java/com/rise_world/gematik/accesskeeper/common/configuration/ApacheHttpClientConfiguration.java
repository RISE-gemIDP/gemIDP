/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("java:S1118") // class needs a public constructor because spring has to instantiate it
@Configuration
@ConditionalOnClass(name =  "org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduit")
public class ApacheHttpClientConfiguration {

    static {
        System.setProperty("org.apache.cxf.transport.http.async.usePolicy", "true");
    }
}

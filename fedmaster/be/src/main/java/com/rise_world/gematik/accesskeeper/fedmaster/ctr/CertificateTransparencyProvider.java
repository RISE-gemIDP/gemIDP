/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.CtRecordDto;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Accessing Certificate Transparency Records from a CTR provider
 */
public interface CertificateTransparencyProvider {

    /**
     * Fetches Certificate Transparency Records for a specific domain
     *
     * @param domain domain to be checked
     * @return list of all matching CT records
     */
    List<CtRecordDto> fetch(String domain);

    /**
     * {@code cacheable} wraps a {@link CertificateTransparencyProvider}
     * and stores the returned values by domain
     *
     * @param provider {@link CertificateTransparencyProvider}
     * @return {@link CertificateTransparencyProvider} backed by a ConcurrentHashMap to store already fetched domains
     */
    static CertificateTransparencyProvider cacheable(CertificateTransparencyProvider provider) {
        var cache = new ConcurrentHashMap<String, List<CtRecordDto>>();
        return domain -> cache.computeIfAbsent(domain, k -> List.copyOf(provider.fetch(k)));
    }
}

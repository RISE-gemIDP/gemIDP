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
}

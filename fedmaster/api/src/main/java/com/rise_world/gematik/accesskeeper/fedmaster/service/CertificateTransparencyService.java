/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

import java.time.Instant;

/**
 * Service to expose certificate transparency monitoring
 */
public interface CertificateTransparencyService {

    /**
     * Validates TLS public keys of a registered participant
     * against CTR provider
     *
     * @param id                id of a registered participant
     * @param monitoringTime    reference timestamp for ctr check (used for filtering outdated ctr entries).
     *                          the timestamp will be stored as lastMonitoredAt after a successful check
     */
    void checkParticipant(Long id, Instant monitoringTime);
}

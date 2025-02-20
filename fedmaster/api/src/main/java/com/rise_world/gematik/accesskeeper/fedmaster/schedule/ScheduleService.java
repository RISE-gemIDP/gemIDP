/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.schedule;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.SynchronizationDto;

/**
 * Schedule service is used to schedule tasks executed by {@code db-scheduler}
 */
public interface ScheduleService {

    void scheduleCtrMonitoring(Long participantId, CTRProvider ctrProvider);

    void scheduleSynchronization(SynchronizationDto synchronization);

    void scheduleFederationMasterMonitoring(CTRProvider ctrProvider);
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.actuator;

import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import com.rise_world.gematik.accesskeeper.fedmaster.service.EntityStatementSynchronization;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "schedule")
public class ScheduleActuatorEndpoint {

    private final EntityStatementSynchronization scheduler;

    public ScheduleActuatorEndpoint(@Qualifier("scheduled") EntityStatementSynchronization scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Schedule tasks/actions for a specific participant
     * @param action        action that should be applied to participant
     * @param identifier    for the participant to be monitored
     * @return id to identify in logs
     */
    @WriteOperation
    public String schedule(@Selector SchedulerAction action, @Selector Long identifier) {
        switch (action) {
            case SYNC -> scheduler.synchronizeParticipant(identifier, true, false);
            case CTR -> scheduler.synchronizeParticipant(identifier, false, true);
            case ALL -> scheduler.synchronizeParticipant(identifier, true, true);
            default -> throw new IllegalStateException("Unexpected value: " + action);
        }

        return MDC.get(LogTool.MDC_TRACE_ID);
    }
}

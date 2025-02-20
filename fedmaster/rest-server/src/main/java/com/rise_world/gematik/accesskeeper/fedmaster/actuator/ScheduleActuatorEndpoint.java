/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.actuator;

import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.SynchronizationDto;
import com.rise_world.gematik.accesskeeper.fedmaster.schedule.CTRProvider;
import com.rise_world.gematik.accesskeeper.fedmaster.schedule.ScheduleService;
import jakarta.ws.rs.QueryParam;
import org.slf4j.MDC;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "schedule")
public class ScheduleActuatorEndpoint {

    private final ScheduleService scheduleService;

    public ScheduleActuatorEndpoint(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * Schedule tasks/actions for a specific participant
     *
     * @param action      action that should be applied to participant
     * @param identifier  for the participant to be monitored
     * @param ctrProvider optional {@link CTRProvider} to use for monitoring. If null, monitoring for all providers will be executed
     * @return id to identify in logs
     */
    @WriteOperation
    public String schedule(@Selector SchedulerAction action,
                    @Selector @Nullable  Long identifier,
                    @QueryParam("ctrProvider") @Nullable String ctrProvider) {

        switch (action) {
            case SYNC -> scheduleService.scheduleSynchronization(synchronization(identifier));
            case CTR -> scheduleService.scheduleCtrMonitoring(identifier, CTRProvider.byId(ctrProvider));
            case ALL -> {
                scheduleService.scheduleSynchronization(synchronization(identifier));
                scheduleService.scheduleCtrMonitoring(identifier, CTRProvider.byId(ctrProvider));
            }
            default -> throw new IllegalStateException("Unexpected value: " + action);
        }

        return MDC.get(LogTool.MDC_TRACE_ID);
    }

    // parameter selfCheck is required for spring to get a separate path
    @SuppressWarnings("unused")
    @WriteOperation
    public String selfCheck(@Selector SelfCheck unused,
                     @QueryParam("ctrProvider") @Nullable String ctrProvider) {
        scheduleService.scheduleFederationMasterMonitoring(CTRProvider.byId(ctrProvider));
        return MDC.get(LogTool.MDC_TRACE_ID);
    }

    private static SynchronizationDto synchronization(Long identifier) {
        var synchronization = new SynchronizationDto();
        synchronization.setParticipantId(identifier);
        synchronization.setTraceId(MDC.get(LogTool.MDC_TRACE_ID));
        synchronization.setSpanId(MDC.get(LogTool.MDC_SPAN_ID));

        return synchronization;
    }

    public enum SelfCheck {
        SELF
    }
}

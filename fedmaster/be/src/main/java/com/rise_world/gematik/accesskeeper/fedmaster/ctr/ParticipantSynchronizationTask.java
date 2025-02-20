/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.StateReturningExecutionHandler;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.SynchronizationDto;
import com.rise_world.gematik.accesskeeper.fedmaster.service.EntityStatementSynchronization;
import com.rise_world.gematik.accesskeeper.fedmaster.util.SynchronizationLog;
import org.springframework.stereotype.Service;

import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.OK;
import static java.util.Objects.nonNull;

@Service
public class ParticipantSynchronizationTask implements StateReturningExecutionHandler<SynchronizationDto> {

    private final EntityStatementSynchronization synchronization;

    ParticipantSynchronizationTask(EntityStatementSynchronization synchronization) {
        this.synchronization = synchronization;
    }

    @Override
    public SynchronizationDto execute(TaskInstance<SynchronizationDto> instance, ExecutionContext executionContext) {
        try {
            LogTool.startTrace(instance.getData().getTraceId());
            LogTool.startSpan(instance.getData().getSpanId());
            SynchronizationLog.log(OK, "Starting synchronization. instance: {}, data: {}", instance, instance.getData());

            if (nonNull(instance.getData().getParticipantId())) {
                synchronization.synchronizeParticipant(instance.getData().getParticipantId());
            }
            else {
                synchronization.synchronize();
            }

            return new SynchronizationDto();
        }
        finally {
            // tracing information needs to be cleared if no existing trace has been used
            LogTool.clearTracingInformation();
        }
    }
}

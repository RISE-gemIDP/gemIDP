/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.schedule;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import com.rise_world.gematik.accesskeeper.fedmaster.ctr.CertificateTransparencyTaskFactory;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.SynchronizationDto;
import com.rise_world.gematik.accesskeeper.fedmaster.service.EntityStatementSynchronization;
import jakarta.annotation.PostConstruct;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private final Scheduler scheduler;
    private final Clock clock;
    private final CertificateTransparencyTaskFactory certificateTransparencyTaskFactory;

    public ScheduleServiceImpl(Scheduler scheduler, Clock clock, CertificateTransparencyTaskFactory certificateTransparencyTaskFactory) {
        this.scheduler = scheduler;
        this.clock = clock;
        this.certificateTransparencyTaskFactory = certificateTransparencyTaskFactory;
    }

    @PostConstruct
    void setupSchedules() {
        scheduler.schedule(certificateTransparencyTaskFactory.federationMasterCheckInstance(CTRProvider.SSL_MATE), clock.instant());
        scheduler.schedule(certificateTransparencyTaskFactory.federationMasterCheckInstance(CTRProvider.CRT_SH), clock.instant());
    }

    @Override
    public void scheduleCtrMonitoring(Long participantId, CTRProvider provider) {
        if (nonNull(provider)) {
            scheduler.reschedule(TaskInstanceId.of(CertificateTransparencyTaskFactory.name(provider), RecurringTask.INSTANCE),
                    clock.instant(),
                    CertificateTransparencyCheck.participant(
                            participantId,
                            MDC.get(LogTool.MDC_TRACE_ID),
                            MDC.get(LogTool.MDC_SPAN_ID)));

            return;
        }

        Stream.of(CTRProvider.values()).forEach(p -> scheduleCtrMonitoring(participantId, p));
    }

    @Override
    public void scheduleSynchronization(SynchronizationDto synchronization) {
        scheduler.reschedule(TaskInstanceId.of(EntityStatementSynchronization.INSTANCE_NAME, RecurringTask.INSTANCE), clock.instant(), synchronization);
    }

    @Override
    public void scheduleFederationMasterMonitoring(CTRProvider provider) {
        if (nonNull(provider)) {
            scheduler.reschedule(certificateTransparencyTaskFactory.federationMasterCheckInstance(provider), clock.instant());
            return;
        }

        Stream.of(CTRProvider.values()).forEach(this::scheduleFederationMasterMonitoring);
    }
}

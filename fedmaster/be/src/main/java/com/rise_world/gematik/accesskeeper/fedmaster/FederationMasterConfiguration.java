/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import com.github.kagkarlsson.scheduler.serializer.Serializer;
import com.github.kagkarlsson.scheduler.task.StateReturningExecutionHandler;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.SynchronizationDto;
import com.rise_world.gematik.accesskeeper.fedmaster.exception.SchedulingException;
import com.rise_world.gematik.accesskeeper.fedmaster.service.EntityStatementSynchronization;
import com.rise_world.gematik.accesskeeper.fedmaster.service.SynchronizationConfiguration;
import com.rise_world.gematik.accesskeeper.fedmaster.util.SynchronizationLog;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.rise_world.gematik.accesskeeper.fedmaster.service.EntityStatementSynchronization.INSTANCE_NAME;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.OK;

@Configuration
@ConfigurationProperties(prefix = "federation")
public class FederationMasterConfiguration {

    public static final String USER_AGENT = "gematik Federation Master";

    private String issuer;

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration tokenTimeout;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Duration getTokenTimeout() {
        return tokenTimeout;
    }

    public void setTokenTimeout(Duration tokenTimeout) {
        this.tokenTimeout = tokenTimeout;
    }

    @Bean
    public DbSchedulerCustomizer dbSchedulerCustomizer() {
        return new DbSchedulerCustomizer() {
            @Override
            public Optional<Serializer> serializer() {
                // define jackson serializer for db scheduler
                return Optional.of(new JacksonSerializer());
            }
        };
    }

    @Bean
    public StateReturningExecutionHandler<SynchronizationDto> getExecutionHandler(EntityStatementSynchronization synchronization) {
        return (instance, ctx) -> {
            try {
                LogTool.startTrace(instance.getData().getTraceId());
                LogTool.startSpan(instance.getData().getSpanId());
                SynchronizationLog.log(OK, "Starting synchronization. instance: {}, data: {}", instance, instance.getData());

                if (instance.getData().getParticipantId() != null) {
                    synchronization.synchronizeParticipant(instance.getData().getParticipantId(),
                        instance.getData().isSynchronization(),
                        instance.getData().isMonitoring());
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
        };
    }

    @Bean
    public Task<SynchronizationDto> synchronizationTask(StateReturningExecutionHandler<SynchronizationDto> handler,
                                                        SynchronizationConfiguration configuration) {
        return Tasks.recurring(INSTANCE_NAME, Schedules.fixedDelay(configuration.getInterval()), SynchronizationDto.class)
            .initialData(new SynchronizationDto())
            .executeStateful(handler);
    }

    @Bean
    @Qualifier("scheduled")
    public EntityStatementSynchronization scheduled(Clock clock, Scheduler scheduler) {
        return new EntityStatementSynchronization() {
            @Override
            public void synchronize() {
                throw new UnsupportedOperationException("full synchronization not allowed for rescheduling");
            }

            @Override
            public void synchronizeParticipant(Long identifier, boolean needsDataSync, boolean needsCtMonitoring) {
                SynchronizationDto data = new SynchronizationDto();
                data.setTraceId(MDC.get(LogTool.MDC_TRACE_ID));
                data.setSpanId(MDC.get(LogTool.MDC_SPAN_ID));
                data.setParticipantId(identifier);
                data.setSynchronization(needsDataSync);
                data.setMonitoring(needsCtMonitoring);

                try {
                    scheduler.reschedule(TaskInstanceId.of(INSTANCE_NAME, RecurringTask.INSTANCE), clock.instant(), data);
                }
                catch (Exception e) {
                    throw new SchedulingException("Rescheduling failed", e);
                }
            }
        };
    }
}

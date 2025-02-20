/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import com.github.kagkarlsson.scheduler.serializer.Serializer;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.rise_world.gematik.accesskeeper.common.service.SynchronizationConfiguration;
import com.rise_world.gematik.accesskeeper.fedmaster.ctr.CertificateTransparencyTaskFactory;
import com.rise_world.gematik.accesskeeper.fedmaster.ctr.FederationMasterCheck;
import com.rise_world.gematik.accesskeeper.fedmaster.ctr.ParticipantSynchronizationTask;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.SynchronizationDto;
import com.rise_world.gematik.accesskeeper.fedmaster.schedule.CTRProvider;
import com.rise_world.gematik.accesskeeper.fedmaster.schedule.CertificateTransparencyCheck;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.rise_world.gematik.accesskeeper.fedmaster.service.EntityStatementSynchronization.INSTANCE_NAME;

@Configuration(proxyBeanMethods = false)
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
                return Optional.of(new JacksonSerializer(mapper -> mapper
                    .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
                    .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
                    .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)));
            }
        };
    }

    @Bean
    public Task<CertificateTransparencyCheck> sslMateCertificateCheckTask(CertificateTransparencyTaskFactory taskFactory) {
        return taskFactory.create(CTRProvider.SSL_MATE);
    }

    @Bean
    public Task<CertificateTransparencyCheck> crtShCertificateCheckTask(CertificateTransparencyTaskFactory taskFactory) {
        return taskFactory.create(CTRProvider.CRT_SH);
    }

    @Bean
    public Task<FederationMasterCheck> federationMasterCheckTask(CertificateTransparencyTaskFactory taskFactory) {
        return taskFactory.federationMasterCheck();
    }

    @Bean
    public Task<SynchronizationDto> synchronizationTask(ParticipantSynchronizationTask task,
                                                        SynchronizationConfiguration configuration) {
        return Tasks.recurring(INSTANCE_NAME, Schedules.fixedDelay(configuration.getInterval()), SynchronizationDto.class)
            .initialData(new SynchronizationDto())
            .executeStateful(task);
    }

}

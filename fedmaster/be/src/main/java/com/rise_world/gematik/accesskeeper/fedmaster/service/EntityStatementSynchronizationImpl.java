/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

import com.rise_world.gematik.accesskeeper.common.service.SynchronizationConfiguration;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantType;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.ParticipantRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.util.CtrCheckLog;
import com.rise_world.gematik.accesskeeper.fedmaster.util.SynchronizationLog;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.rise_world.gematik.accesskeeper.fedmaster.service.Severity.ERROR;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.OK;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.TECHNICAL;
import static com.rise_world.gematik.accesskeeper.fedmaster.util.MarkerUtils.appendParticipant;
import static net.logstash.logback.marker.Markers.appendEntries;

@Service
@Primary
public class EntityStatementSynchronizationImpl implements EntityStatementSynchronization {

    private final ParticipantRepository participantRepository;
    private final SynchronizationConfiguration configuration;
    private final SynchronizationService service;
    private final Clock clock;
    private final CertificateTransparencyConfiguration ctConfiguration;
    private final CertificateTransparencyService ctService;

    public EntityStatementSynchronizationImpl(Clock clock,
                                              SynchronizationConfiguration configuration,
                                              CertificateTransparencyConfiguration ctConfiguration,
                                              ParticipantRepository participantRepository,
                                              SynchronizationService service,
                                              CertificateTransparencyService ctService) {
        this.configuration = configuration;
        this.ctConfiguration = ctConfiguration;
        this.participantRepository = participantRepository;
        this.service = service;
        this.ctService = ctService;
        this.clock = clock;
    }

    @Override
    public void synchronize() {
        Instant synchronizationTime = Instant.now(clock);
        SynchronizationLog.log(OK, "synchronization job started at {}", synchronizationTime);
        CtrCheckLog.log(OK, "monitoring job started at {}", synchronizationTime);

        Date syncedBefore = Date.from(synchronizationTime.minus(configuration.getExpiration()));
        Date monitoredBefore = Date.from(synchronizationTime.minus(ctConfiguration.getExpiration()));
        List<ParticipantDto> toBeSynced = participantRepository.findBeforeSyncAt(syncedBefore, monitoredBefore);

        long toBeSyncedCount = toBeSynced.stream().filter(e -> e.getSynchronizedAt().before(syncedBefore)).count();
        long toBeMonitoredCount = toBeSynced.stream().filter(e -> e.getType() == ParticipantType.OP && e.getLastMonitoredAt().before(monitoredBefore)).count();

        SynchronizationLog.log(OK, "{} participant(s) found for synchronization", toBeSyncedCount);
        CtrCheckLog.log(OK, "{} participant(s) found for monitoring", toBeMonitoredCount);

        for (ParticipantDto participant : toBeSynced) {
            if (participant.getSynchronizedAt().before(syncedBefore)) {
                doSynchronize(participant, synchronizationTime);
            }

            if (participant.getLastMonitoredAt().before(monitoredBefore)) {
                doMonitoring(participant, synchronizationTime);
            }
        }

        SynchronizationLog.log(OK, "finished synchronization successfully");
        CtrCheckLog.log(OK, "finished monitoring successfully");
    }

    @Override
    public void synchronizeParticipant(Long identifier, boolean dataSync, boolean ctrCheck) {
        Instant synchronizationTime = Instant.now(clock);

        Optional<ParticipantDto> participant = participantRepository.findById(identifier);
        if (participant.isPresent()) {
            if (dataSync) {
                SynchronizationLog.log(OK, "synchronization job started at {}", synchronizationTime);
                doSynchronize(participant.get(), synchronizationTime);
                SynchronizationLog.log(OK, "finished synchronization successfully");
            }
            if (ctrCheck) {
                CtrCheckLog.log(OK, "monitoring job started at {}", synchronizationTime);
                doMonitoring(participant.get(), synchronizationTime);
                CtrCheckLog.log(OK, "finished monitoring successfully");
            }
        }
        else {
            if (dataSync) {
                SynchronizationLog.log(TECHNICAL, "participant with identifier {} not found", identifier);
            }
            if (ctrCheck) {
                CtrCheckLog.log(TECHNICAL, "participant with identifier {} not found", identifier);
            }
        }
    }

    private void doMonitoring(ParticipantDto participantDto, Instant synchronizationTime) {
        try {
            ctService.checkParticipant(participantDto.getId(), synchronizationTime);
        }
        catch (Exception ex) {
            CtrCheckLog.log(TECHNICAL, appendParticipant(participantDto),
                "certificate transparency monitoring for participant {} failed - reason: {}", participantDto.getSub(), ex.getMessage(), ex);
        }
    }

    private void doSynchronize(ParticipantDto participant, Instant synchronizationTime) {
        try {
            this.service.synchronizeParticipant(participant, synchronizationTime);
        }
        catch (SynchronizationException ex) {
            if (ex.getStatusCode().getSeverity() == ERROR) {
                SynchronizationLog.log(ex.getStatusCode(), appendParticipant(participant)
                    .and(appendEntries(ex.getRelated())),
                    "synchronization for participant {} failed - reason: {}", participant.getSub(), ex.getMessage(), ex);
            }
            else {
                SynchronizationLog.log(ex.getStatusCode(), appendParticipant(participant)
                    .and(appendEntries(ex.getRelated())),
                    "synchronization for participant {} currently not possible - reason: {}", participant.getSub(), ex.getMessage(), ex);
            }
        }
        catch (Exception ex) {
            SynchronizationLog.log(TECHNICAL, appendParticipant(participant), "synchronization for participant {} failed - reason: {}", participant.getSub(), ex.getMessage(), ex);
        }
        finally {
            this.service.logSynchronizationRun(participant, synchronizationTime);
        }
    }

}

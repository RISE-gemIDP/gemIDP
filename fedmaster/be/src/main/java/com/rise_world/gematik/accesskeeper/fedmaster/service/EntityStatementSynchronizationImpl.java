/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

import com.rise_world.gematik.accesskeeper.common.service.SynchronizationConfiguration;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.ParticipantRepository;
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
    private final boolean lockRelyingParty;

    public EntityStatementSynchronizationImpl(Clock clock,
                                              SynchronizationConfiguration configuration,
                                              ParticipantRepository participantRepository,
                                              SynchronizationService service) {
        this.configuration = configuration;
        this.participantRepository = participantRepository;
        this.service = service;
        this.clock = clock;
        this.lockRelyingParty = configuration.lockRelyingParty();
    }

    @Override
    public void synchronize() {
        Instant synchronizationTime = Instant.now(clock);
        SynchronizationLog.log(OK, "synchronization job started at {}", synchronizationTime);

        List<ParticipantDto> toBeSynced = loadParticipants(synchronizationTime);
        SynchronizationLog.log(OK, "{} participant(s) found for synchronization", toBeSynced.size());

        toBeSynced.forEach(participant -> doSynchronize(participant, synchronizationTime));
        SynchronizationLog.log(OK, "finished synchronization successfully");
    }

    private List<ParticipantDto> loadParticipants(Instant synchronizationTime) {
        Date syncedBefore = Date.from(synchronizationTime.minus(configuration.getExpiration()));

        if (lockRelyingParty) {
            return participantRepository.findBeforeSyncAtWithInactive(syncedBefore);
        }

        return participantRepository.findBeforeSyncAt(syncedBefore);
    }

    @Override
    public void synchronizeParticipant(Long identifier) {
        Instant synchronizationTime = Instant.now(clock);
        loadParticipant(identifier)
            .ifPresentOrElse(participant -> {
                    SynchronizationLog.log(OK, "synchronization job started at {}", synchronizationTime);
                    doSynchronize(participant, synchronizationTime);
                    SynchronizationLog.log(OK, "finished synchronization successfully");
                },
                () -> SynchronizationLog.log(TECHNICAL, "participant with identifier {} not found", identifier));
    }

    private Optional<ParticipantDto> loadParticipant(Long identifier) {
        if (lockRelyingParty) {
            return participantRepository.findByIdWithInactive(identifier);
        }

        return participantRepository.findById(identifier);
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

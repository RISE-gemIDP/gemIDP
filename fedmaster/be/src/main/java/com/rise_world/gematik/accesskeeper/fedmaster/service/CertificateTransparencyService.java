/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

import com.rise_world.gematik.accesskeeper.fedmaster.ctr.CertificateTransparencyProvider;
import com.rise_world.gematik.accesskeeper.fedmaster.ctr.CertificateTransparencyProviderException;
import com.rise_world.gematik.accesskeeper.fedmaster.ctr.CtrServiceException;
import com.rise_world.gematik.accesskeeper.fedmaster.ctr.FederationMasterDomainCheckConfiguration;
import com.rise_world.gematik.accesskeeper.fedmaster.ctr.RejectedCtrRequestException;
import com.rise_world.gematik.accesskeeper.fedmaster.ctr.RequestLimitExceededException;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.CertificatePublicKeyDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.CtRecordDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDomainDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantType;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.DomainRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.ParticipantRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.PublicKeyRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.schedule.CTRProvider;
import com.rise_world.gematik.accesskeeper.fedmaster.util.CtrCheckLog;
import com.rise_world.gematik.accesskeeper.fedmaster.util.MarkerUtils;
import com.rise_world.gematik.accesskeeper.fedmaster.util.PemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.NO_CERT;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.OK;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.REJECTED;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.SERVICE_ERROR;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.TECHNICAL;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.UNKNOWN_KEY;
import static com.rise_world.gematik.accesskeeper.fedmaster.util.MarkerUtils.appendParticipant;
import static net.logstash.logback.marker.Markers.append;

public class CertificateTransparencyService {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateTransparencyService.class);

    private final CertificateTransparencyConfiguration configuration;
    private final DomainRepository domainRepository;
    private final PublicKeyRepository pukRepository;
    private final ParticipantRepository participantRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final CertificateTransparencyProvider certificateTransparencyProvider;
    private final CtrCheckLog ctrCheckLog;
    private final CTRProvider provider;

    public CertificateTransparencyService(CertificateTransparencyConfiguration configuration,
                                          DomainRepository domainRepository,
                                          PublicKeyRepository pukRepository,
                                          ParticipantRepository participantRepository,
                                          Clock clock,
                                          TransactionTemplate transactionTemplate,
                                          CertificateTransparencyProvider certificateTransparencyProvider,
                                          CtrCheckLog ctrCheckLog,
                                          CTRProvider provider) {
        this.domainRepository = domainRepository;
        this.pukRepository = pukRepository;
        this.participantRepository = participantRepository;
        this.configuration = configuration;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
        this.certificateTransparencyProvider = certificateTransparencyProvider;
        this.ctrCheckLog = ctrCheckLog;
        this.provider = provider;
    }

    public CheckStatus checkParticipants() {

        var monitoringTime = clock.instant();
        logMonitoringStart(ctrCheckLog, monitoringTime);

        var expiration = monitoringTime.minus(configuration.getExpiration());
        var participantsToMonitor = participantRepository.findBeforeMonitoredAt(expiration, provider);
        ctrCheckLog.log(OK, "{} participant(s) found for monitoring", participantsToMonitor.size());

        if (participantsToMonitor.isEmpty()) {
            logSuccess(ctrCheckLog);
            return CheckStatus.NO_CHECK;
        }

        var status = CheckStatus.NOK;
        try {
            for (var participant : participantsToMonitor) {
                var participantStatus = doCheck(participant, monitoringTime);
                status = CheckStatus.accumulate(status, participantStatus);
            }

            logSuccess(ctrCheckLog);
        }
        catch (RequestLimitExceededException e) {
            ctrCheckLog.log(OK, "certificate transparency check request limit exceeded - aborting check");
        }

        return status;
    }

    public CheckStatus checkParticipant(Long id) {

        var monitoringTime = clock.instant();
        logMonitoringStart(ctrCheckLog, monitoringTime);

        try {
            var result = participantRepository.findById(id)
                .filter(participant -> participant.getType() == ParticipantType.OP)
                .map(participant -> doCheck(participant, monitoringTime));

            if (result.isEmpty()) {
                ctrCheckLog.log(TECHNICAL, "Participant '{}' is not a registered openid provider", id);
            }

            logSuccess(ctrCheckLog);
            return result.orElse(CheckStatus.NO_CHECK);
        }
        catch (RequestLimitExceededException e) {
            ctrCheckLog.log(OK, "certificate transparency check request limit exceeded");
            return CheckStatus.NOK;
        }
    }

    private CheckStatus doCheck(ParticipantDto participant, Instant monitoringTime) {
        return transactionTemplate.execute(trx -> checkTransactional(participant, monitoringTime));
    }

    private CheckStatus checkTransactional(ParticipantDto participant, Instant monitoringTime) {
        var domainsOfParticipant = domainRepository.findByParticipant(participant.getId());
        var certificatePuks = pukRepository.findAllCertificateKeysByParticipant(participant.getId());

        for (ParticipantDomainDto domain : domainsOfParticipant) {
            try {
                checkDomain(monitoringTime, participant, domain.getName(), certificatePuks);
            }
            catch (RejectedCtrRequestException e) {
                ctrCheckLog.log(REJECTED, appendParticipant(participant)
                        .and(append(MarkerUtils.DOMAIN, domain.getName())),
                    e.getMessage(), e);
                // CTR check failed - no further domain checks will be performed

                return CheckStatus.NOK;
            }
            catch (CtrServiceException e) {
                ctrCheckLog.log(SERVICE_ERROR, e.getMessage(), e);
                // CTR check failed - no further domain checks will be performed
                return CheckStatus.NOK;
            }
            catch (CertificateTransparencyProviderException e) {
                LOG.error("Certificate transparency provider access failed - reason: {}", e.getMessage(), e);
                // CTR check failed - no further domain checks will be performed
                return CheckStatus.NOK;
            }
        }

        // set
        participantRepository.setMonitoringRun(participant.getId(), Timestamp.from(monitoringTime), provider);
        return CheckStatus.OK;
    }

    private void checkDomain(Instant now, ParticipantDto participant, String domain, List<CertificatePublicKeyDto> puks) {

        List<CtRecordDto> records = certificateTransparencyProvider.fetch(domain).stream()
            .filter(e -> isActive(now, e))
            .toList();

        if (records.isEmpty()) {
            ctrCheckLog.log(NO_CERT, appendParticipant(participant)
                    .and(append(MarkerUtils.DOMAIN, domain)),
                "no records found for domain {}", domain);
            return;
        }

        Set<String> pubKeyHashes = puks.stream()
            .filter(key -> domain.equalsIgnoreCase(key.getDomain()))
            .map(e -> this.extractHash(participant, e, ctrCheckLog))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        List<String> unknownKeys = new ArrayList<>();
        for (CtRecordDto ctr : records) {
            ctrCheckLog.log(OK, append(MarkerUtils.PUBKEY_SHA_256, ctr.pubKeyHash())
                    .and(append(MarkerUtils.ISSUER, ctr.issuerName()))
                    .and(append(MarkerUtils.DOMAIN, String.join(", ", ctr.dnsNames())))
                    .and(append(MarkerUtils.NOT_BEFORE, ctr.notBefore().toString()))
                    .and(append(MarkerUtils.NOT_AFTER, ctr.notAfter().toString())),
                "Certificate Transparency Record for domain {}: {}", domain, ctr);

            if (!pubKeyHashes.contains(ctr.pubKeyHash())) {
                unknownKeys.add(ctr.pubKeyHash());
            }
        }

        if (!unknownKeys.isEmpty()) {
            ctrCheckLog.log(UNKNOWN_KEY, appendParticipant(participant)
                    .and(append(MarkerUtils.DOMAIN, domain))
                    .and(append(MarkerUtils.MEMBER_PUBKEY_SHA_256, String.join(", ", unknownKeys))),
                "Certificate Transparency Record does not match information registered on federation master");
        }
    }

    private String extractHash(ParticipantDto participant, CertificatePublicKeyDto puk, CtrCheckLog ctrCheckLog) {
        String hash = PemUtils.hashKey(puk.getPem());
        String domain = puk.getDomain();
        ctrCheckLog.log(OK, appendParticipant(participant)
                .and(append(MarkerUtils.DOMAIN, domain))
                .and(append(MarkerUtils.MEMBER_PUBKEY_SHA_256, hash)),
            "FederationMaster registered key for participant on domain {} - {}", domain, hash);
        return hash;
    }

    private boolean isActive(Instant now, CtRecordDto ctRecordDto) {
        if (ctRecordDto.revoked()) {
            LOG.debug("record {} has been revoked", ctRecordDto);
            return false;
        }
        else if (now.isBefore(ctRecordDto.notBefore())) {
            LOG.debug("record {} is not active yet", ctRecordDto);
            return false;
        }
        else if (now.isAfter(ctRecordDto.notAfter())) {
            LOG.debug("record {} is expired", ctRecordDto);
            return false;
        }
        return true;
    }

    private static void logMonitoringStart(CtrCheckLog ctrCheckLog, Instant start) {
        ctrCheckLog.log(OK, "monitoring job started at {}", start);
    }

    private static void logSuccess(CtrCheckLog ctrCheckLog) {
        ctrCheckLog.log(OK, "finished monitoring successfully");
    }

    public CheckStatus checkFederationMaster(String domain, FederationMasterDomainCheckConfiguration config) {

        if (config.getPublicKeys().isEmpty()) {
            ctrCheckLog.log(OK, "no certificates configured for federation master");
            return CheckStatus.NO_CHECK;
        }

        List<CtRecordDto> records;
        try {
            records = certificateTransparencyProvider.fetch(domain);
        }
        catch (RejectedCtrRequestException e) {
            ctrCheckLog.log(REJECTED, append(MarkerUtils.DOMAIN, domain), e.getMessage(), e);
            return CheckStatus.NOK;
        }
        catch (CtrServiceException e) {
            ctrCheckLog.log(SERVICE_ERROR, e.getMessage(), e);
            return CheckStatus.NOK;
        }
        catch (CertificateTransparencyProviderException e) {
            LOG.error("Certificate transparency provider access failed - reason: {}", e.getMessage(), e);
            return CheckStatus.NOK;
        }

        if (records.isEmpty()) {
            ctrCheckLog.log(NO_CERT, append(MarkerUtils.DOMAIN, domain), "no records found for domain {}", domain);
            return CheckStatus.OK;
        }

        var unknownKeys = new ArrayList<String>(records.size());
        for (var ctr : records) {
            ctrCheckLog.log(OK, append(MarkerUtils.PUBKEY_SHA_256, ctr.pubKeyHash())
                            .and(append(MarkerUtils.ISSUER, ctr.issuerName()))
                            .and(append(MarkerUtils.DOMAIN, String.join(", ", ctr.dnsNames())))
                            .and(append(MarkerUtils.NOT_BEFORE, ctr.notBefore().toString()))
                            .and(append(MarkerUtils.NOT_AFTER, ctr.notAfter().toString())),
                    "Certificate Transparency Record for domain {}: {}", domain, ctr);

            var hash = ctr.pubKeyHash();
            if (!config.getPublicKeys().contains(hash)) {
                unknownKeys.add(hash);
            }
        }

        if (!unknownKeys.isEmpty()) {
            ctrCheckLog.log(UNKNOWN_KEY,
                append(MarkerUtils.MEMBER_URI, domain)
                    .and(append(MarkerUtils.ZIS_ASSIGNMENT_GROUP, config.getZisGroup()))
                    .and(append(MarkerUtils.DOMAIN, domain))
                    .and(append(MarkerUtils.MEMBER_PUBKEY_SHA_256, String.join(", ", unknownKeys))),
                "Certificate Transparency Record does not match information configured for federation master");
        }

        return CheckStatus.OK;
    }

}

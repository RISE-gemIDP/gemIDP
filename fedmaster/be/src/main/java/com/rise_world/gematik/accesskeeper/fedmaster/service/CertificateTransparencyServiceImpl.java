/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

import com.rise_world.gematik.accesskeeper.fedmaster.ctr.CertificateTransparencyProvider;
import com.rise_world.gematik.accesskeeper.fedmaster.ctr.CertificateTransparencyProviderException;
import com.rise_world.gematik.accesskeeper.fedmaster.ctr.CtrServiceException;
import com.rise_world.gematik.accesskeeper.fedmaster.ctr.RejectedCtrRequestException;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.CertificatePublicKeyDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.CtRecordDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDomainDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantType;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.DomainRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.ParticipantRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.PublicKeyRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.util.CtrCheckLog;
import com.rise_world.gematik.accesskeeper.fedmaster.util.MarkerUtils;
import com.rise_world.gematik.accesskeeper.fedmaster.util.PemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.NO_CERT;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.OK;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.REJECTED;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.SERVICE_ERROR;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.UNKNOWN_KEY;
import static com.rise_world.gematik.accesskeeper.fedmaster.util.MarkerUtils.appendParticipant;
import static net.logstash.logback.marker.Markers.append;

@Service
public class CertificateTransparencyServiceImpl implements CertificateTransparencyService {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateTransparencyServiceImpl.class);

    private final CertificateTransparencyProvider provider;
    private final DomainRepository domainRepository;
    private final PublicKeyRepository pukRepository;
    private final ParticipantRepository participantRepository;

    public CertificateTransparencyServiceImpl(DomainRepository domainRepository,
                                              PublicKeyRepository pukRepository,
                                              ParticipantRepository participantRepository,
                                              CertificateTransparencyProvider provider) {
        this.domainRepository = domainRepository;
        this.pukRepository = pukRepository;
        this.participantRepository = participantRepository;
        this.provider = provider;
    }

    @Override
    @Transactional
    public void checkParticipant(Long id, Instant monitoringTime) {
        Optional<ParticipantDto> participantDto = participantRepository.findById(id);
        if (participantDto.isEmpty() || participantDto.get().getType() != ParticipantType.OP) {
            LOG.info("Participant '{}' is not a registered openid provider", id);
            return;
        }

        List<ParticipantDomainDto> domainsOfParticipant = domainRepository.findByParticipant(id);
        List<CertificatePublicKeyDto> certificatePuks = pukRepository.findAllCertificateKeysByParticipant(id);

        for (ParticipantDomainDto domain : domainsOfParticipant) {
            try {
                checkDomain(monitoringTime, participantDto.get(), domain.getName(), certificatePuks);
            }
            catch (RejectedCtrRequestException e) {
                CtrCheckLog.log(REJECTED, appendParticipant(participantDto.get())
                    .and(append(MarkerUtils.DOMAIN, domain.getName())),
                    e.getMessage(), e);
                // CTR check failed - no further domain checks will be performed
                return;
            }
            catch (CtrServiceException e) {
                CtrCheckLog.log(SERVICE_ERROR, e.getMessage(), e);
                // CTR check failed - no further domain checks will be performed
                return;
            }
            catch (CertificateTransparencyProviderException e) {
                LOG.error("Certificate transparency provider access failed - reason: {}", e.getMessage(), e);
                // CTR check failed - no further domain checks will be performed
                return;
            }
        }

        // set
        participantRepository.setMonitoringRun(id, Timestamp.from(monitoringTime));
    }

    private void checkDomain(Instant now, ParticipantDto participant, String domain, List<CertificatePublicKeyDto> puks) {
        List<CtRecordDto> records = provider.fetch(domain).stream()
            .filter(e -> isActive(now, e))
            .toList();

        if (records.isEmpty()) {
            CtrCheckLog.log(NO_CERT, appendParticipant(participant)
                .and(append(MarkerUtils.DOMAIN, domain)),
                "no records found for domain {}", domain);
            return;
        }

        Set<String> pubKeyHashes = puks.stream()
            .filter(key -> domain.equalsIgnoreCase(key.getDomain()))
            .map(e -> this.extractHash(participant, e))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        List<String> unknownKeys = new ArrayList<>();
        for (CtRecordDto ctr : records) {
            CtrCheckLog.log(OK, append(MarkerUtils.PUBKEY_SHA_256, ctr.pubKeyHash())
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
            CtrCheckLog.log(UNKNOWN_KEY, appendParticipant(participant)
                    .and(append(MarkerUtils.DOMAIN, domain))
                    .and(append(MarkerUtils.MEMBER_PUBKEY_SHA_256, String.join(", ", unknownKeys))),
                "Certificate Transparency Record does not match information registered on federation master");
        }
    }

    private String extractHash(ParticipantDto participant, CertificatePublicKeyDto puk) {
        String hash = PemUtils.hashKey(puk.getPem());
        String domain = puk.getDomain();
        CtrCheckLog.log(OK, appendParticipant(participant)
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
}

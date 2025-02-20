/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

import com.rise_world.gematik.accesskeeper.fedmaster.repository.DomainRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.ParticipantRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.PublicKeyRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.schedule.CTRProvider;
import com.rise_world.gematik.accesskeeper.fedmaster.service.CertificateTransparencyConfiguration;
import com.rise_world.gematik.accesskeeper.fedmaster.service.CertificateTransparencyService;
import com.rise_world.gematik.accesskeeper.fedmaster.util.CtrCheckLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

/**
 * Factory to create a {@link CertificateTransparencyService} used during a monitoring job run ({@link CertificateTransparencyCheckTask})
 */
@Service
public class CertificateTransparencyServiceFactory {

    private final CertificateTransparencyConfiguration config;
    private final DomainRepository domainRepository;
    private final PublicKeyRepository pukRepository;
    private final ParticipantRepository participantRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final SslMateProvider sslMateProvider;
    private final CrtShProvider crtShProvider;

    public CertificateTransparencyServiceFactory(CertificateTransparencyConfiguration config,
                                                 DomainRepository domainRepository,
                                                 PublicKeyRepository pukRepository,
                                                 ParticipantRepository participantRepository,
                                                 Clock clock,
                                                 TransactionTemplate transactionTemplate,
                                                 SslMateProvider sslMateProvider,
                                                 CrtShProvider crtShProvider) {
        this.config = config;
        this.domainRepository = domainRepository;
        this.pukRepository = pukRepository;
        this.participantRepository = participantRepository;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
        this.sslMateProvider = sslMateProvider;
        this.crtShProvider = crtShProvider;
    }

    /**
     * {@code create} creates a {@link CertificateTransparencyService} for the given {@code provider} with the
     * {@link CtrCheckLog} to use for a monitoring run.
     *
     * @param provider {@link CTRProvider}
     * @param ctrCheckLog {@link CtrCheckLog} to use for the monitoring run
     *
     * @return {@link CertificateTransparencyService} configured for the given {@code provider}
     */
    public CertificateTransparencyService create(CTRProvider provider, CtrCheckLog ctrCheckLog) {
        return new CertificateTransparencyService(config,
            domainRepository,
            pukRepository,
            participantRepository,
            clock,
            transactionTemplate,
            CertificateTransparencyProvider.cacheable(ctrProvider(provider)),
            ctrCheckLog,
            provider);
    }

    private CertificateTransparencyProvider ctrProvider(CTRProvider provider) {
        return switch (provider) {
            case SSL_MATE -> sslMateProvider;
            case CRT_SH -> crtShProvider;
        };
    }
}

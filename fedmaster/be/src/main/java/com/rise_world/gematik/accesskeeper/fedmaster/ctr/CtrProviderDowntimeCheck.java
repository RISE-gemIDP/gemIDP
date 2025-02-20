/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

import com.rise_world.gematik.accesskeeper.fedmaster.repository.CtrCheckLogRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.schedule.CTRProvider;
import com.rise_world.gematik.accesskeeper.fedmaster.service.CertificateTransparencyConfiguration;
import com.rise_world.gematik.accesskeeper.fedmaster.service.CheckStatus;
import com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode;
import com.rise_world.gematik.accesskeeper.fedmaster.util.CtrCheckLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class CtrProviderDowntimeCheck {

    private final CtrCheckLogRepository ctrCheckLogRepository;
    private final CertificateTransparencyConfiguration config;
    private final Clock clock;

    public CtrProviderDowntimeCheck(CtrCheckLogRepository ctrCheckLogRepository, CertificateTransparencyConfiguration config, Clock clock) {
        this.ctrCheckLogRepository = ctrCheckLogRepository;
        this.config = config;
        this.clock = clock;
    }

    /**
     * {@code handleStatus} takes actions depending on the given {@code status} for the monitoring job
     *
     * <ul>
     *     <li>{@link CheckStatus#OK} records the success timestamp for the given {@link CTRProvider provider}</li>
     *     <li>{@link CheckStatus#NOK} creates an incident if the max downtime for the given {@link CTRProvider provider} is exceeded</li>
     *     <li>{@link CheckStatus#NO_CHECK} no action is performed</li>
     * </ul>
     *
     * @param providerType {@link CTRProvider}
     * @param status       {@link CheckStatus}
     * @param ctrCheckLog  {@link CtrCheckLog} to create the incident
     */
    @Transactional
    public void handleStatus(CTRProvider providerType, CheckStatus status, CtrCheckLog ctrCheckLog) {

        if (status == CheckStatus.NO_CHECK) {
            return;
        }

        if (status == CheckStatus.OK) {
            ctrCheckLogRepository.saveSuccess(providerType, clock.instant());
            return;
        }

        ctrCheckLogRepository.lastSuccess(providerType)
            .filter(lastSuccess -> lastSuccess.isBefore(clock.instant().minus(config.getMaxDowntime())))
            .ifPresent(lastSuccess -> ctrCheckLog.log(StatusCode.MAX_DOWNTIME_REACHED, "max downtime reached - last successful check: {}", lastSuccess));
    }

}

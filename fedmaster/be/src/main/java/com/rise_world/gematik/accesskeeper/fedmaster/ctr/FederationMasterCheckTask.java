/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.StateReturningExecutionHandler;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.rise_world.gematik.accesskeeper.fedmaster.util.CtrCheckLog;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.OK;

public class FederationMasterCheckTask implements StateReturningExecutionHandler<FederationMasterCheck> {

    private final CertificateTransparencyServiceFactory serviceFactory;
    private final CertificateTransparencyTaskFactory certificateTransparencyTaskFactory;
    private final CtrProviderDowntimeCheck downtimeCheck;
    private final FederationMasterDomainCheckConfiguration config;
    private final String federationMasterDomain;

    public FederationMasterCheckTask(CertificateTransparencyServiceFactory serviceFactory,
                                     CertificateTransparencyTaskFactory certificateTransparencyTaskFactory,
                                     CtrProviderDowntimeCheck downtimeCheck,
                                     FederationMasterDomainCheckConfiguration config,
                                     String federationMasterDomain) {
        this.serviceFactory = serviceFactory;
        this.certificateTransparencyTaskFactory = certificateTransparencyTaskFactory;
        this.downtimeCheck = downtimeCheck;
        this.config = config;
        this.federationMasterDomain = federationMasterDomain;
    }

    @Override
    public FederationMasterCheck execute(TaskInstance<FederationMasterCheck> taskInstance, ExecutionContext executionContext) {

        var provider = taskInstance.getData().getData();
        var ctrCheckLog = new CtrCheckLog(provider);
        ctrCheckLog.log(OK, "Starting ctr-monitoring for federation master. provider: {}", provider);

        var service = serviceFactory.create(provider, ctrCheckLog);

        var status = service.checkFederationMaster(federationMasterDomain, config);

        downtimeCheck.handleStatus(provider, status, ctrCheckLog);
        ctrCheckLog.log(OK, "finished monitoring successfully");

        return switch (status) {
            case OK -> certificateTransparencyTaskFactory.federationMasterCheckInstance(provider).getData();
            case NOK, NO_CHECK -> new FederationMasterCheck(Schedules.fixedDelay(Duration.of(5, ChronoUnit.MINUTES)), provider);
        };
    }
}

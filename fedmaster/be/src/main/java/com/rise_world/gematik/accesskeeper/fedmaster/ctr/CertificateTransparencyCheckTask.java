/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.StateReturningExecutionHandler;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.rise_world.gematik.accesskeeper.common.util.LogTool;
import com.rise_world.gematik.accesskeeper.fedmaster.schedule.CTRProvider;
import com.rise_world.gematik.accesskeeper.fedmaster.schedule.CertificateTransparencyCheck;
import com.rise_world.gematik.accesskeeper.fedmaster.service.CheckStatus;
import com.rise_world.gematik.accesskeeper.fedmaster.util.CtrCheckLog;

import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.OK;
import static java.util.Objects.isNull;

/**
 * The {@code CertificateTransparencyCheckTask} is scheduled via the {@code db-scheduler} library.
 */
public class CertificateTransparencyCheckTask implements StateReturningExecutionHandler<CertificateTransparencyCheck> {

    private final CTRProvider providerType;
    private final CertificateTransparencyServiceFactory serviceFactory;
    private final CtrProviderDowntimeCheck ctrProviderDowntimeCheck;

    public CertificateTransparencyCheckTask(CTRProvider providerType,
                                            CertificateTransparencyServiceFactory serviceFactory,
                                            CtrProviderDowntimeCheck ctrProviderDowntimeCheck) {
        this.providerType = providerType;
        this.serviceFactory = serviceFactory;
        this.ctrProviderDowntimeCheck = ctrProviderDowntimeCheck;
    }

    @Override
    public CertificateTransparencyCheck execute(TaskInstance<CertificateTransparencyCheck> taskInstance, ExecutionContext executionContext) {
        var certificateTransparencyCheck = taskInstance.getData();

        try {
            LogTool.startTrace(certificateTransparencyCheck.getTraceId());
            LogTool.startSpan(certificateTransparencyCheck.getSpanId());

            var ctrCheckLog = new CtrCheckLog(providerType);

            ctrCheckLog.log(OK, "Starting ctr-monitoring. instance: {}, provider: {}, data: {}", taskInstance, providerType, certificateTransparencyCheck);

            var status = executeCheck(certificateTransparencyCheck, ctrCheckLog);
            ctrProviderDowntimeCheck.handleStatus(providerType, status, ctrCheckLog);
            return CertificateTransparencyCheck.outdatedParticipants();
        }
        finally {
            LogTool.clearTracingInformation();
        }
    }

    private CheckStatus executeCheck(CertificateTransparencyCheck certificateTransparencyCheck, CtrCheckLog ctrCheckLog) {

        var service = serviceFactory.create(providerType, ctrCheckLog);

        if (isNull(certificateTransparencyCheck.getParticipantId())) {
            return service.checkParticipants();
        }

        return service.checkParticipant(certificateTransparencyCheck.getParticipantId());
    }

}

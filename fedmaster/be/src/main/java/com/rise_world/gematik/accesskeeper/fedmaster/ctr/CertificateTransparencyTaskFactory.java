/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.TaskWithDataDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.rise_world.gematik.accesskeeper.common.service.SynchronizationConfiguration;
import com.rise_world.gematik.accesskeeper.fedmaster.schedule.CTRProvider;
import com.rise_world.gematik.accesskeeper.fedmaster.schedule.CertificateTransparencyCheck;
import com.rise_world.gematik.accesskeeper.fedmaster.service.CertificateTransparencyConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;

/**
 * Factory to create a {@link CertificateTransparencyCheckTask} for a specific {@link CTRProvider provider}
 */
@Service
public class CertificateTransparencyTaskFactory {

    private static final TaskWithDataDescriptor<FederationMasterCheck> FEDERATION_MASTER_CHECK_TASK =
        new TaskWithDataDescriptor<>("federation-master-ctr-check", FederationMasterCheck.class);

    private static final String TASK_NAME_TEMPLATE = "ctr-check-%s";

    private final SynchronizationConfiguration config;
    private final CertificateTransparencyConfiguration ctrConfig;
    private final CertificateTransparencyServiceFactory serviceFactory;
    private final CtrProviderDowntimeCheck ctrProviderDowntimeCheck;
    private final FederationMasterDomainCheckConfiguration fedMasterCheckConfig;
    private final String federationMaster;

    CertificateTransparencyTaskFactory(SynchronizationConfiguration config,
                                       CertificateTransparencyConfiguration ctrConfig,
                                       CertificateTransparencyServiceFactory serviceFactory,
                                       CtrProviderDowntimeCheck ctrProviderDowntimeCheck,
                                       FederationMasterDomainCheckConfiguration fedMasterCheckConfig,
                                       @Value("${federation.issuer}") String federationMaster) {
        this.config = config;
        this.ctrConfig = ctrConfig;
        this.serviceFactory = serviceFactory;
        this.ctrProviderDowntimeCheck = ctrProviderDowntimeCheck;
        this.fedMasterCheckConfig = fedMasterCheckConfig;
        this.federationMaster = federationMaster;
    }

    /**
     * {@code creates} configures a {@link Task scheduled task} for the certificate transparency monitoring job
     *
     * @param provider {@link CTRProvider}
     * @return {@link com.github.kagkarlsson.scheduler.task.helper.RecurringTask task} configured with the given {@link CTRProvider provider}
     */
    public Task<CertificateTransparencyCheck> create(CTRProvider provider) {
        return Tasks.recurring(name(provider),
                Schedules.fixedDelay(config.getInterval()), CertificateTransparencyCheck.class)
            .initialData(CertificateTransparencyCheck.outdatedParticipants())
            .executeStateful(new CertificateTransparencyCheckTask(provider, serviceFactory, ctrProviderDowntimeCheck));
    }

    /**
     * {@code federMasterCheck} creates the recurring task to check the federation master domain
     *
     * @return {@link com.github.kagkarlsson.scheduler.task.helper.RecurringTaskWithPersistentSchedule task}
     */
    public Task<FederationMasterCheck> federationMasterCheck() {

        var domain = URI.create(federationMaster).getHost();

        return Tasks.recurringWithPersistentSchedule(FEDERATION_MASTER_CHECK_TASK)
            .executeStateful(new FederationMasterCheckTask(serviceFactory, this, ctrProviderDowntimeCheck, fedMasterCheckConfig, domain));
    }

    /**
     * {@code federationMasterCheckInstance} creates a task instance for the given {@link CTRProvider}
     * with the schedule ({@link CertificateTransparencyConfiguration#getExpiration()})
     *
     * @param provider {@link CTRProvider} for the task instance
     * @return {@link TaskInstance} for the given {@link CTRProvider}
     */
    public TaskInstance<FederationMasterCheck> federationMasterCheckInstance(CTRProvider provider) {
        return FEDERATION_MASTER_CHECK_TASK.instance(provider.getId(),
            new FederationMasterCheck(Schedules.fixedDelay(ctrConfig.getExpiration()), provider));
    }

    /**
     * generates the task name for the given {@link CTRProvider}
     *
     * @param provider {@link CTRProvider}
     * @return task name
     */
    public static String name(CTRProvider provider) {
        return TASK_NAME_TEMPLATE.formatted(provider.getId());
    }


}

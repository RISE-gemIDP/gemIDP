/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

import com.github.kagkarlsson.scheduler.task.helper.ScheduleAndData;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import com.rise_world.gematik.accesskeeper.fedmaster.schedule.CTRProvider;

public class FederationMasterCheck implements ScheduleAndData {

    private Schedule schedule;
    private CTRProvider provider;

    @SuppressWarnings("unused")
    FederationMasterCheck() {
        // constructor for jackson
    }

    public FederationMasterCheck(Schedule schedule, CTRProvider provider) {
        this.schedule = schedule;
        this.provider = provider;
    }

    @Override
    public Schedule getSchedule() {
        return schedule;
    }

    @Override
    public CTRProvider getData() {
        return provider;
    }
}

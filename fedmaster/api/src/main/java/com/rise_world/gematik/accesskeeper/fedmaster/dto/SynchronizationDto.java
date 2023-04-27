/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.dto;

import java.io.Serializable;

public class SynchronizationDto implements Serializable {

    private boolean synchronization = false;
    private boolean monitoring = false;
    private String traceId;
    private String spanId;
    private Long participantId;

    public boolean isSynchronization() {
        return synchronization;
    }

    public void setSynchronization(boolean synchronization) {
        this.synchronization = synchronization;
    }

    public boolean isMonitoring() {
        return monitoring;
    }

    public void setMonitoring(boolean monitoring) {
        this.monitoring = monitoring;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public Long getParticipantId() {
        return participantId;
    }

    public void setParticipantId(Long participantId) {
        this.participantId = participantId;
    }

    @Override
    public String toString() {
        return "SynchronizationDto{" +
            "synchronization=" + synchronization +
            ", monitoring=" + monitoring +
            ", traceId='" + traceId + '\'' +
            ", spanId='" + spanId + '\'' +
            ", participantId=" + participantId +
            '}';
    }
}

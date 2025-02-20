/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.schedule;

import java.util.Objects;

/**
 * CertificateTransparencyCheck contains execution parameters for {@code CertificateTransparencyCheckTask}
 */
public class CertificateTransparencyCheck {

    private String traceId;
    private String spanId;
    private Long participantId;

    CertificateTransparencyCheck() {
        // default constructor needed for jackson
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public Long getParticipantId() {
        return participantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CertificateTransparencyCheck that = (CertificateTransparencyCheck) o;
        return Objects.equals(participantId, that.participantId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(participantId);
    }

    @Override
    public String toString() {
        return "CertificateTransparencyCheck{" +
                "participantId=" + participantId +
                '}';
    }

    /**
     * {@code outdatedParticipants()} creates a {@link CertificateTransparencyCheck} to check all outdated participants
     *
     * @return {@link CertificateTransparencyCheck} without participant restriction
     */
    public static CertificateTransparencyCheck outdatedParticipants() {
        return new CertificateTransparencyCheck();
    }

    /**
     * {@code participant} forces a {@link CertificateTransparencyCheck} for the given {@code participantId}
     *
     * @param participantId id of the participant to force the CTR-Check
     * @param traceId       traceId of the force request
     * @param spanId        spanId of the force request
     * @return forced {@link CertificateTransparencyCheck} for the given {@code participantId}
     */
    public static CertificateTransparencyCheck participant(long participantId, String traceId, String spanId) {
        var check = new CertificateTransparencyCheck();
        check.traceId = traceId;
        check.spanId = spanId;
        check.participantId = participantId;
        return check;
    }
}

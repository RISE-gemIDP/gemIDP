/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.dto;

import java.sql.Timestamp;

public class ParticipantDto {

    private Long id;
    private String sub;
    private ParticipantType type;
    private boolean active;
    private String organizationName;
    private String clientName;
    private String logoUri;
    private String userTypeSupported;
    private Timestamp synchronizedAt;
    private Timestamp lastScheduledRun;
    private String zisGroup;
    private boolean pkv;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public ParticipantType getType() {
        return type;
    }

    public void setType(ParticipantType type) {
        this.type = type;
    }

    public boolean isLocked() {
        return !isActive();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getLogoUri() {
        return logoUri;
    }

    public void setLogoUri(String logoUri) {
        this.logoUri = logoUri;
    }

    public String getUserTypeSupported() {
        return userTypeSupported;
    }

    public void setUserTypeSupported(String userTypeSupported) {
        this.userTypeSupported = userTypeSupported;
    }

    public Timestamp getSynchronizedAt() {
        return synchronizedAt;
    }

    public void setSynchronizedAt(Timestamp synchronizedAt) {
        this.synchronizedAt = synchronizedAt;
    }

    public Timestamp getLastScheduledRun() {
        return lastScheduledRun;
    }

    public void setLastScheduledRun(Timestamp lastScheduledRun) {
        this.lastScheduledRun = lastScheduledRun;
    }

    public String getZisGroup() {
        return zisGroup;
    }

    public void setZisGroup(String zisGroup) {
        this.zisGroup = zisGroup;
    }

    public boolean isPkv() {
        return pkv;
    }

    public void setPkv(boolean pkv) {
        this.pkv = pkv;
    }
}

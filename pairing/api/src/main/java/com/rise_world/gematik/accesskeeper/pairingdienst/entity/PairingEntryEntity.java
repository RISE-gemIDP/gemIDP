/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.entity;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Database entity for "Pairing".
 * <p>
 * A "Pairing" is a binding of a device (hardware) and a cryptographic key to a certain entity (typically a human user).
 * Such an entity can own multiple "Pairings".
 */
public class PairingEntryEntity {

    // synthetic key
    private final Long id;

    // anonymized id number
    private final String idNummer;

    private final String keyIdentifier;

    private final String pairingEntryVersion;

    private final String deviceName;

    private final Timestamp creationTime;

    private final String signedPairingData;

    private PairingEntryEntity(Long id, String idNummer, String keyIdentifier, String pairingEntryVersion, String deviceName,
                               Timestamp creationTime, String signedPairingData) {
        this.id = id;
        this.idNummer = idNummer;
        this.keyIdentifier = keyIdentifier;
        this.pairingEntryVersion = pairingEntryVersion;
        this.deviceName = deviceName;
        this.creationTime = creationTime;
        this.signedPairingData = signedPairingData;
    }

    /**
     * Returns a builder for {@code PairingEntryEntity}.
     *
     * @return a new builder
     */
    public static PairingEntryEntityBuilder aPairingEntryEntity() {
        return new PairingEntryEntityBuilder();
    }

    public Long getId() {
        return id;
    }

    public String getIdNummer() {
        return idNummer;
    }

    public String getKeyIdentifier() {
        return keyIdentifier;
    }

    public String getPairingEntryVersion() {
        return pairingEntryVersion;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public Timestamp getCreationTime() {
        return creationTime;
    }

    public Instant getCreationTimeAsInstant() {
        return creationTime.toInstant();
    }

    public String getSignedPairingData() {
        return signedPairingData;
    }

    public static final class PairingEntryEntityBuilder {

        private Long id;

        private String idNummer;
        private String keyIdentifier;

        private String pairingEntryVersion;
        private String deviceName;
        private Timestamp creationTime;
        private String signedPairingData;

        private PairingEntryEntityBuilder() {
            // avoid instantiation
        }

        // CHECKSTYLE:OFF
        // suppress MissingJavadocMethod
        public PairingEntryEntityBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public PairingEntryEntityBuilder withIdNummer(String idNummer) {
            this.idNummer = idNummer;
            return this;
        }

        public PairingEntryEntityBuilder withKeyIdentifier(String keyIdentifier) {
            this.keyIdentifier = keyIdentifier;
            return this;
        }

        public PairingEntryEntityBuilder withPairingEntryVersion(String pairingEntryVersion) {
            this.pairingEntryVersion = pairingEntryVersion;
            return this;
        }

        public PairingEntryEntityBuilder withDeviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        public PairingEntryEntityBuilder withCreationTime(Timestamp creationTime) {
            this.creationTime = creationTime;
            return this;
        }

        public PairingEntryEntityBuilder withSignedPairingData(String signedPairingData) {
            this.signedPairingData = signedPairingData;
            return this;
        }

        public PairingEntryEntity build() {
            return new PairingEntryEntity(id, idNummer, keyIdentifier, pairingEntryVersion, deviceName, creationTime, signedPairingData);
        }
        // CHECKSTYLE:ON
    }
}

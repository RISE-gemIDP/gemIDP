/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.service;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.CertificateServiceException;

import java.time.Instant;

/**
 * Provides methods for certificate validation against OCSP.
 */
public interface CertificateServiceClient {

    /**
     * Validate the given certificate.
     *
     * @param referenceDate the reference date which is used to check the validity of a certificate
     * @param autCert       EGK, HBA or SMCB certificate (base64 encoded with padding)
     * @throws CertificateServiceException if the remote CertificateService returned an error
     * @throws AccessKeeperException       if the remote CertificateService is not available or a technical error occurred
     */
    void validateClientCertificateAgainstOCSP(Instant referenceDate, String autCert);

    /**
     * Validate the given certificate.
     *
     * @param referenceDate the reference date which is used to check the validity of a certificate
     * @param autCert       EGK, HBA or SMCB certificate
     * @throws CertificateServiceException if the remote CertificateService returned an error
     * @throws AccessKeeperException       if the remote CertificateService is not available or a technical error occurred
     */
    void validateClientCertificateAgainstOCSP(Instant referenceDate, byte[] autCert);
}

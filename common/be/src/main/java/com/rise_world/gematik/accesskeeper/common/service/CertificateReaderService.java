/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.service;

import com.rise_world.gematik.accesskeeper.common.dto.CardType;
import com.rise_world.gematik.accesskeeper.common.exception.CertReaderException;

import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * Provides methods for reading and parsing AUT certificates
 */
public interface CertificateReaderService {

    /**
     * Parses a byte array into a X509Certificate object
     *
     * @param pem the certificate bytes
     * @return the parsed object
     * @throws CertReaderException if the certificate can not be parsed
     */
    X509Certificate parseCertificate(byte[] pem) throws CertReaderException;

    /**
     * Parses a base64 encoded certificate into a X509Certificate object
     *
     * @param pem the base64 encoded certificate
     * @return the parsed object
     * @throws CertReaderException if the certificate can not be parsed
     */
    X509Certificate parseCertificate(String pem) throws CertReaderException;

    /**
     * Parses the policy extension of the certificate and reads the OID
     *
     * @param x509Certificate the AUT certificate
     * @return the type of the AUT certificate
     * @throws CertReaderException if the certificate is not one of the 3 expected AUT certificates
     */
    CardType getCardType(X509Certificate x509Certificate) throws CertReaderException;

    /**
     * Extracts the attribute "idNummer" from a X509 certificate.
     *
     * @param certificate the AUT certificate
     * @return idNummer
     * @throws CertReaderException if idNummer extraction failed
     */
    String extractIdNummer(X509Certificate certificate) throws CertReaderException;

    /**
     * Extracts the certificate claims (given name, family name, id number, organisation and profession) from the certificate
     *
     * @param certificate the AUT certificate
     * @return the extracted claims
     * @throws CertReaderException if the claim extraction failed
     */
    Map<String, String> extractCertificateClaims(X509Certificate certificate) throws CertReaderException;
}

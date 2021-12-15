/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rise_world.gematik.accesskeeper.common.OAuth2Constants;
import com.rise_world.gematik.accesskeeper.common.crypt.DecryptionProviderFactory;
import com.rise_world.gematik.accesskeeper.common.crypt.IdNummerAnonymizer;
import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.CertReaderException;
import com.rise_world.gematik.accesskeeper.common.exception.CertificateServiceException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;
import com.rise_world.gematik.accesskeeper.common.service.CertificateReaderService;
import com.rise_world.gematik.accesskeeper.common.service.CertificateServiceClient;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.EpkValidation;
import com.rise_world.gematik.accesskeeper.pairingdienst.dto.AccessTokenDTO;
import com.rise_world.gematik.accesskeeper.pairingdienst.dto.AuthenticationDataDTO;
import com.rise_world.gematik.accesskeeper.pairingdienst.dto.DeviceStatus;
import com.rise_world.gematik.accesskeeper.pairingdienst.dto.DeviceTypeDTO;
import com.rise_world.gematik.accesskeeper.pairingdienst.dto.RegistrationDataDTO;
import com.rise_world.gematik.accesskeeper.pairingdienst.entity.PairingEntryEntity;
import com.rise_world.gematik.accesskeeper.pairingdienst.exception.ErrorDetails;
import com.rise_world.gematik.accesskeeper.pairingdienst.exception.PairingDienstException;
import com.rise_world.gematik.accesskeeper.pairingdienst.repository.BlockAllowListRepository;
import com.rise_world.gematik.accesskeeper.pairingdienst.repository.PairingRepository;
import com.rise_world.gematik.accesskeeper.pairingdienst.service.exception.InvalidSignedPairingDataException;
import com.rise_world.gematik.accesskeeper.pairingdienst.service.validation.Validations;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweCompactConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionOutput;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweException;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.interfaces.ECPublicKey;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes.REG1_CLIENT_ERROR;
import static com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes.REG2_DEVICE_BLACKLISTED;
import static com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes.REG3_REGISTRATION_SERVER_ERROR;
import static com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes.VAL1_ALT_AUTH_FAILED;
import static com.rise_world.gematik.accesskeeper.pairingdienst.exception.ErrorDetails.BLOCKED_DEVICE;
import static com.rise_world.gematik.accesskeeper.pairingdienst.exception.ErrorDetails.ID_NUMMER_MISMATCH;
import static com.rise_world.gematik.accesskeeper.pairingdienst.exception.ErrorDetails.INVALID_AUTH_CERT;
import static com.rise_world.gematik.accesskeeper.pairingdienst.exception.ErrorDetails.INVALID_CONTENT;
import static com.rise_world.gematik.accesskeeper.pairingdienst.exception.ErrorDetails.INVALID_HEADER;
import static com.rise_world.gematik.accesskeeper.pairingdienst.exception.ErrorDetails.INVALID_SIGNATURE_SIGNED_AUTHENTICATION_DATA;
import static com.rise_world.gematik.accesskeeper.pairingdienst.exception.ErrorDetails.INVALID_SIGNED_PAIRING_DATA;
import static com.rise_world.gematik.accesskeeper.pairingdienst.exception.ErrorDetails.OCSP_CHECK_FAILED;
import static com.rise_world.gematik.accesskeeper.pairingdienst.exception.ErrorDetails.PAIRING_ENTRY_NOT_FOUND;
import static net.logstash.logback.marker.Markers.append;

/**
 * Implementation of {@code PairingService}.
 */
@Component
public class PairingServiceImpl implements PairingService {

    private static final Logger LOG = LoggerFactory.getLogger(PairingServiceImpl.class);
    private static final Logger SMARTPHONE_LOG = LoggerFactory.getLogger("com.rise_world.gematik.accesskeeper.pairingdienst.service.SmartphoneLog");

    // @AFO: A_21419 - erlaubte Methoden
    private static final List<String> REG_METHODS = Arrays.asList(OAuth2Constants.AMR_MULTI_FACTOR_AUTH, OAuth2Constants.AMR_SMART_CARD, OAuth2Constants.AMR_PIN);

    private static final ErrorMessage AC3_DEREGISTER_SERVER_ERROR = new ErrorMessage(ErrorCodes.AC3_DEREGISTER_USER_ERROR, 500);
    private static final String PAIRING_ENTRY_VERSION = "1.0";
    private static final int DEVICE_EXPIRY_MONTHS = 6;

    private final CertificateReaderService certificateReaderService;
    private final CertificateServiceClient certificateServiceClient;
    private final SignedPairingDataValidator signedPairingDataValidator;
    private final JweDecryptionProvider jweDecryptionProvider;
    private final IdNummerAnonymizer idNummerAnonymizer;
    private final PairingRepository pairingRepository;
    private final BlockAllowListRepository balRepository;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    private final EpkValidation epkValidation = new EpkValidation(ErrorCodes.REG1_CLIENT_ERROR);

    @Autowired
    @SuppressWarnings("squid:S00107") // parameters required for dependency injection
    // CHECKSTYLE:OFF More than 10 parameters
    public PairingServiceImpl(CertificateReaderService certificateReaderService,
                              CertificateServiceClient certificateServiceClient,
                              SignedPairingDataValidator signedPairingDataValidator,
                              DecryptionProviderFactory decryptionProviderFactory,
                              IdNummerAnonymizer idNummerAnonymizer,
                              PairingRepository pairingRepository,
                              BlockAllowListRepository balRepository,
                              Clock clock,
                              ObjectMapper objectMapper) {
        // CHECKSTYLE:ON
        this.certificateServiceClient = certificateServiceClient;
        this.certificateReaderService = certificateReaderService;
        this.pairingRepository = pairingRepository;
        this.balRepository = balRepository;
        // @AFO: A_21420 - Decryption Provider zum Entschl&uuml;sseln  der Registrierungsdaten wird initialisiert
        this.jweDecryptionProvider = decryptionProviderFactory.createDecryptionProvider(TokenType.REGISTRATION_INFO);
        this.idNummerAnonymizer = idNummerAnonymizer;
        this.objectMapper = objectMapper;
        this.signedPairingDataValidator = signedPairingDataValidator;
        this.clock = clock;
    }

    @Override
    // @AFO A_21426 - die verschl&uuml;sselten Registrierungsdaten werden entschl&uuml;sselt und in ein RegistrationDataDTO geparst.
    // Dieses DTO enth&auml;lt unter anderem das AUT Zertifikat und die signierten Pairingdaten.
    // In die PairingEntryEntity wird das AUT Zertifikat nicht &uuml;bernommen
    public PairingEntryEntity registerPairing(AccessTokenDTO accessToken, String encryptedRegistrationData) {
        try {
            verifyRegistrationAuthenticationMethods(accessToken);

            RegistrationDataDTO registrationData = decryptRegistrationData(encryptedRegistrationData);

            DeviceTypeDTO deviceType = registrationData.getDeviceInformation().getDeviceType();
            DeviceStatus deviceStatus = balRepository.fetchDeviceStatus(deviceType);

            SMARTPHONE_LOG.info(append("device_type", deviceType)
                    .and(append("device_request_type", "PAIRING"))
                    .and(append("device_status", deviceStatus)),
                    "register pairing using {} during {} resulting in {}",
                    ToStringBuilder.reflectionToString(deviceType, ToStringStyle.NO_CLASS_NAME_STYLE), "PAIRING", deviceStatus);

            // @AFO: A_21423 Prüfung ob Gerätetyp auf der Block-Liste eingetragen ist und Rückgabe mit entsprechender
            // Fehlermeldung REG.2
            if (deviceStatus == DeviceStatus.BLOCK) {
                LOG.warn("registration not allowed, device is on block list");
                throw new PairingDienstException(REG2_DEVICE_BLACKLISTED);
            }

            String signedPairingData = registrationData.getSignedPairingData();
            AuthCertificate authCertificate = parseAuthCertificate(registrationData.getAuthCertificate(), REG1_CLIENT_ERROR);

            String keyIdentifier = signedPairingDataValidator.validateForRegistration(signedPairingData, authCertificate,
                registrationData.getDeviceInformation().getDeviceType().getProduct());
            // @AFO: A_21421 - das Zertifikat wird anhand der Systemzeit &uuml;berpr&uuml;ft
            validateClientCertificateAgainstOCSP(clock.instant(), authCertificate.asBytes());

            String idNummer = certificateReaderService.extractIdNummer(authCertificate.asX509Certificate());

            // @AFO: A_21422 - idNummer aus dem AUT-Zertifikat wird mit der idNummer aus dem Access-Token verglichen
            if (!Objects.equals(idNummer, accessToken.getIdNummer())) {
                LOG.warn("not matching idNummer of accessToken and authentication certificate");
                throw new PairingDienstException(REG1_CLIENT_ERROR, ID_NUMMER_MISMATCH);
            }

            String anonymizeIdNummer = idNummerAnonymizer.anonymizeIdNummer(idNummer);

            // @AFO: A_21424 - in der Pairing-Tabelle werden der Gerätename, die signierten Pairingdaten und der aktuelle Zeitstempel persistiert
            PairingEntryEntity pairingEntryEntity = PairingEntryEntity.aPairingEntryEntity()
                .withIdNummer(anonymizeIdNummer)
                .withKeyIdentifier(keyIdentifier)
                .withPairingEntryVersion(PAIRING_ENTRY_VERSION)
                .withDeviceName(registrationData.getDeviceInformation().getName())
                .withCreationTime(Timestamp.from(clock.instant()))
                .withSignedPairingData(signedPairingData)
                .build();

            long id = pairingRepository.save(pairingEntryEntity);
            LOG.debug("registered pairing with id={}", id);

            return pairingEntryEntity;
        }
        catch (InvalidSignedPairingDataException e) {
            // @AFO: A_21421 - Fehlerhafte Pairingdaten werden als Fehler REG1. weitergeworfen
            throw new PairingDienstException(REG1_CLIENT_ERROR, INVALID_SIGNED_PAIRING_DATA, e);
        }
        catch (CertReaderException c) {
            LOG.warn("invalid authentication certificate");
            throw new PairingDienstException(REG1_CLIENT_ERROR, INVALID_AUTH_CERT, c);
        }
        catch (PairingDienstException e) {
            throw e;
        }
        catch (DataAccessException e) {
            throw new PairingDienstException(REG3_REGISTRATION_SERVER_ERROR, e);
        }
        catch (Exception e) {
            throw new PairingDienstException(REG1_CLIENT_ERROR, e);
        }
    }

    @Override
    public String verifyAlternativeAuthentication(String signedAuthenticationDataAsString) {
        LOG.debug("Alternative authentication will be verified");

        JwsJwtCompactConsumer signedAuthenticationData;
        AuthenticationDataDTO authenticationData;

        try {
            signedAuthenticationData = new JwsJwtCompactConsumer(signedAuthenticationDataAsString);
            authenticationData = parseAuthenticationData(signedAuthenticationData);
        }
        catch (PairingDienstException e) {
            throw e;
        }
        catch (Exception e) {
            // the received payload cannot be parsed
            throw new PairingDienstException(VAL1_ALT_AUTH_FAILED, INVALID_SIGNATURE_SIGNED_AUTHENTICATION_DATA, e);
        }

        try {
            DeviceTypeDTO deviceType = authenticationData.getDeviceInformation().getDeviceType();
            // @AFO A_21432 Prüfung ob Gerätetyp auf der Block-Liste eingetragen ist und Rückgabe mit entsprechender
            // Fehlermeldung VAL.1
            // @AFO A_21437 Prüfung ob Gerätetyp NICHT auf der Block-Liste eingetragen ist. Verarbeitung wird fortgeführt.
            DeviceStatus deviceStatus = balRepository.fetchDeviceStatus(deviceType);

            SMARTPHONE_LOG.info(append("device_type", deviceType)
                    .and(append("device_request_type", "AUTH"))
                    .and(append("device_status", deviceStatus)),
                    "alternative authorization using {} during {} resulting in {}",
                    ToStringBuilder.reflectionToString(deviceType, ToStringStyle.NO_CLASS_NAME_STYLE), "AUTH",
                    deviceStatus);

            if (deviceStatus == DeviceStatus.BLOCK) {
                LOG.warn("authentication not allowed, device is on block list");
                throw new PairingDienstException(VAL1_ALT_AUTH_FAILED, BLOCKED_DEVICE);
            }

            AuthCertificate authCertificate = parseAuthCertificate(authenticationData.getAuthCertificate(), VAL1_ALT_AUTH_FAILED);

            // extract idNummer and fetch pairing from database
            String idNummer = certificateReaderService.extractIdNummer(authCertificate.asX509Certificate());
            PairingEntryEntity pairingEntryEntity = fetchPairingFromDB(idNummer, authenticationData.getKeyIdentifier());

            // @AFO A_21432 Prüfung ob Gerätetyp weder auf der Block-Liste noch auf der Allow-Liste eingetragen ist
            // und das Alter der Registrierung noch valide ist. Im Falle einer zu alten Registrierung wird mit
            // entsprechender Fehlermeldung VAL.1 geantwortet.
            // @AFO A_21437 Prüfung ob Gerätetyp weder auf der Block-Liste noch auf der Allow-Liste eingetragen ist
            // und das Alter der Registrierung noch valide ist. Falls nicht abgelaufen wird die Verarbeitung fortgeführt.
            if (deviceStatus == DeviceStatus.UNKNOWN && isExpired(pairingEntryEntity)) {
                LOG.warn("authentication not allowed, device is on grey list and is expired");
                throw new PairingDienstException(VAL1_ALT_AUTH_FAILED, BLOCKED_DEVICE);
            }

            ECPublicKey pukSeAut = signedPairingDataValidator.validateForAuthentication(pairingEntryEntity.getSignedPairingData(), authCertificate);

            // @AFO: A_21438 - Die Integrit&auml;t von SignedAuthenticationData wird mithilfe des &ouml;ffentlichen Schl&uuml;ssels aus dem Pairing-Datensatz &uuml;berpr&uuml;ft
            // @AFO: A_21439 - Die &Uuml;berpr&uuml;fung der Signatur erfolgt mittels des Algorithmus ES256, der als Hashfunktion SHA-256 benutzt.
            if (!signedAuthenticationData.verifySignatureWith(pukSeAut, SignatureAlgorithm.ES256)) {
                LOG.warn("invalid signature of signed authentication data");
                throw new PairingDienstException(VAL1_ALT_AUTH_FAILED, INVALID_SIGNATURE_SIGNED_AUTHENTICATION_DATA);
            }

            LOG.debug("Alternative authentication was verified");

            return authenticationData.getChallengeToken();
        }
        catch (CertReaderException e) {
            LOG.warn("invalid authentication certificate");
            throw new PairingDienstException(VAL1_ALT_AUTH_FAILED, INVALID_AUTH_CERT, e);
        }
        catch (InvalidSignedPairingDataException e) {
            throw new PairingDienstException(VAL1_ALT_AUTH_FAILED, INVALID_SIGNED_PAIRING_DATA, e);
        }
        catch (PairingDienstException e) {
            throw e;
        }
        catch (Exception e) {
            throw new PairingDienstException(VAL1_ALT_AUTH_FAILED, e);
        }
    }

    // @AFO: A_21437 Prüfung des Alters der Registrierung
    private boolean isExpired(PairingEntryEntity pairingEntryEntity) {
        Instant pairingEntryEndOfValidity = pairingEntryEntity
            .getCreationTimeAsInstant()
            .atZone(clock.getZone())
            .plusMonths(DEVICE_EXPIRY_MONTHS)
            .plusDays(1)
            .truncatedTo(ChronoUnit.DAYS)
            .toInstant();
        return clock.instant().isAfter(pairingEntryEndOfValidity);
    }

    private AuthenticationDataDTO parseAuthenticationData(JwsJwtCompactConsumer signedAuthenticationData) {
        AuthenticationDataDTO result = objectMapper.convertValue(signedAuthenticationData.getJwtClaims().asMap(), AuthenticationDataDTO.class);

        if (!result.isValid()) {
            LOG.warn("invalid authentication data content");
            throw new PairingDienstException(VAL1_ALT_AUTH_FAILED, INVALID_CONTENT);
        }

        return result;
    }

    // @AFO A_21434 - Anhand der ID-Nummer und dem &uuml;bermittelten keyIdentifier wird der zugeh&ouml;rige Pairingdatensatz geladen. Wurde keiner gefunden
    private PairingEntryEntity fetchPairingFromDB(String idNummer, String keyIdentifier) {
        String anonymizeIdNummer = idNummerAnonymizer.anonymizeIdNummer(idNummer);

        return pairingRepository.fetchPairing(anonymizeIdNummer, keyIdentifier)
            .orElseThrow(() -> {
                // @AFO A_21434 - Fehler falls kein Eintrag gefunden wurde
                LOG.warn("found no matching pairing in database");
                return new PairingDienstException(VAL1_ALT_AUTH_FAILED, PAIRING_ENTRY_NOT_FOUND);
            });
    }

    private AuthCertificate parseAuthCertificate(String authCertificate, ErrorMessage errorMessage) {
        return new AuthCertificate(authCertificate, i -> {
            try {
                return certificateReaderService.parseCertificate(i);
            }
            catch (CertReaderException e) {
                LOG.warn("failed to parse authentication certificate");
                throw new PairingDienstException(errorMessage, INVALID_AUTH_CERT, e);
            }
        });
    }

    // @AFO: A_21420 - Registrierungsdaten werden entschl&uuml;sselt
    private RegistrationDataDTO decryptRegistrationData(String encryptedRegistrationData) throws IOException {
        JweHeaders jweHeaders;
        try {
            JweCompactConsumer headerConsumer = new JweCompactConsumer(encryptedRegistrationData);
            jweHeaders = headerConsumer.getJweHeaders();
        }
        catch (JweException e) {
            LOG.warn("failed to parse jwe headers", e);
            throw new PairingDienstException(REG1_CLIENT_ERROR, INVALID_HEADER);
        }

        try {
            epkValidation.validate(jweHeaders);
        }
        catch (AccessKeeperException a) {
            throw new PairingDienstException(REG1_CLIENT_ERROR, INVALID_HEADER, a);
        }

        if (!ClaimUtils.hasJoseType(jweHeaders, JoseType.JWT)) {
            LOG.warn("invalid registration data header: type {}", jweHeaders.getHeader(JoseConstants.HEADER_TYPE));
            throw new PairingDienstException(REG1_CLIENT_ERROR, INVALID_HEADER);
        }

        if (!StringUtils.equals(jweHeaders.getContentType(), "JSON")) {
            LOG.warn("invalid registration data header: content type {}", jweHeaders.getContentType());
            throw new PairingDienstException(REG1_CLIENT_ERROR, INVALID_HEADER);
        }

        JweDecryptionOutput registrationData = jweDecryptionProvider.decrypt(encryptedRegistrationData);

        RegistrationDataDTO result = objectMapper.readValue(registrationData.getContent(), RegistrationDataDTO.class);

        if (!result.isValid()) {
            LOG.warn("invalid registration data content");
            throw new PairingDienstException(REG1_CLIENT_ERROR, INVALID_CONTENT);
        }

        return result;
    }

    // @AFO: 21421 - TUC_PKI_018 &Uuml;berpr&uuml;fung des Zertifikats via CertificateService
    private void validateClientCertificateAgainstOCSP(Instant referenceDate, byte[] authCertificate) {
        try {
            certificateServiceClient.validateClientCertificateAgainstOCSP(referenceDate, authCertificate);
        }
        catch (AccessKeeperException | CertificateServiceException e) {
            LOG.warn("OCSP validation of authentication certificate failed");
            throw new PairingDienstException(REG1_CLIENT_ERROR, OCSP_CHECK_FAILED, e);
        }
        catch (Exception e) {
            throw new PairingDienstException(REG3_REGISTRATION_SERVER_ERROR, OCSP_CHECK_FAILED, e);
        }
    }

    // @AFO: A_21452 Bereitstellen der Pairing Datensätze zu einer idNummer
    @Override
    public List<PairingEntryEntity> inspectPairings(AccessTokenDTO accessToken) {
        try {
            String anonymizedIdNummer = idNummerAnonymizer.anonymizeIdNummer(accessToken.getIdNummer());
            return pairingRepository.fetchPairings(anonymizedIdNummer);
        }
        catch (Exception ex) {
            throw new PairingDienstException(ErrorCodes.AC2_NO_DATA_ACCESS, ex);
        }
    }

    // @AFO: A_21448 Löschung des Pairing Datensatzes und Fehlerbehandlung, falls kein Datensatz gelöscht werden konnte
    @Override
    public void deregisterPairing(AccessTokenDTO accessToken, String keyIdentifier) {
        if (!Validations.KEY_IDENTIFIER.isValid(keyIdentifier)) {
            throw new PairingDienstException(ErrorCodes.AC3_DEREGISTER_USER_ERROR, ErrorDetails.INVALID_CONTENT);
        }

        boolean pairingDeleted = false;
        try {
            String anonymizedIdNummer = idNummerAnonymizer.anonymizeIdNummer(accessToken.getIdNummer());
            pairingDeleted = pairingRepository.deletePairing(anonymizedIdNummer, keyIdentifier);
        }
        catch (Exception e) {
            throw new PairingDienstException(AC3_DEREGISTER_SERVER_ERROR, e);
        }

        if (!pairingDeleted) {
            LOG.warn("pairing was not found/deleted");
            throw new PairingDienstException(ErrorCodes.AC3_DEREGISTER_USER_ERROR, ErrorDetails.PAIRING_ENTRY_NOT_FOUND);
        }
    }

    // @AFO: A_21419 - AMR Pr&uuml;fung auf [&bdquo;mfa&ldquo;,&ldquo;sc&ldquo;,&ldquo;pin&ldquo;]
    private void verifyRegistrationAuthenticationMethods(AccessTokenDTO accessTokenDTO) {
        final List<String> methods = accessTokenDTO.getAmr();

        if (methods.size() != REG_METHODS.size() || !methods.containsAll(REG_METHODS)) {
            LOG.warn("authentication methods for pairing registration need to be: mfa, sc, pin");
            throw new PairingDienstException(REG1_CLIENT_ERROR);
        }
    }

}


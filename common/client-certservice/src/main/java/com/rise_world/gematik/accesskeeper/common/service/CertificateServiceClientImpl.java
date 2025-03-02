/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.service;

import com.rise_world.epa.certificate.api.rest.api.CertificateNonQesApi;
import com.rise_world.epa.certificate.api.rest.model.CertificateCheckRequest;
import com.rise_world.epa.certificate.api.rest.model.CertificateCheckResponse;
import com.rise_world.epa.certificate.api.rest.model.ErrorMessage;
import com.rise_world.epa.certificate.api.rest.model.ParsedOcspResponse;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.CertificateServiceException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Service
public class CertificateServiceClientImpl implements CertificateServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateServiceClientImpl.class);
    private static final List<String> POLICY_LIST = Arrays.asList(OidType.OID_C_CH_AUTH, OidType.OID_C_HP_AUTH, OidType.OID_C_HCI_AUTH);
    private static final int CERT_SERVICE_OCSP_NOT_AVAILABLE_CODE = 1032;

    private final CertificateNonQesApi certificateResource;
    private final long ocspGracePeriod;

    @Autowired
    public CertificateServiceClientImpl(CertificateNonQesApi certificateResource,
                                        @Value("${certificateService.ocspGracePeriod}") long ocspGracePeriod) {
        this.certificateResource = certificateResource;
        this.ocspGracePeriod = ocspGracePeriod;
    }


    @Override
    public void validateClientCertificateAgainstOCSP(Instant referenceDate, String autCert) {
        if (autCert == null) {
            LOG.warn("auth certificate is missing");
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_X509_CERT);
        }
        validateClientCertificateAgainstOCSP(referenceDate, Base64.getDecoder().decode(autCert));
    }

    @Override
    public void validateClientCertificateAgainstOCSP(Instant referenceDate, byte[] autCert) {
        if (autCert == null) {
            LOG.warn("auth certificate is missing");
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_X509_CERT);
        }
        CertificateCheckRequest request = new CertificateCheckRequest();

        // @AFO: A_4957-01 Es wird der Status von genau einem Zertifikat abgefragt
        request.setCertificate(autCert);

        request.setIntendedKeyUsage(1); // digital signature
        request.setIntendedExtendedKeyUsage(Collections.singletonList("1.3.6.1.5.5.7.3.2")); //  id-kp-clientAuth
        request.setAllowMissingExtendedKeyUsage(Boolean.TRUE);

        request.setPolicyList(POLICY_LIST);
        request.setCheckType(CertificateCheckRequest.CheckTypeEnum.OCSP);
        request.setOfflineMode(Boolean.FALSE);

        request.setReferenceDate(referenceDate);

        // @AFO: A_4943 OCSP grace period konfigurierbar
        request.setOcspGracePeriod(ocspGracePeriod);

        try {
            // @AFO: A_4637, A_4829 TUC Prüfung und Fehlerbehandlung
            // @AFO: A_20465 das CertificateService prüft immer gegen den zugehörigen Responder
            CertificateCheckResponse response = certificateResource.checkX509(request);

            // @AFO: A_4751 Fehlercodes gemäß Tab_PKI_274 sind in CertificateCheckResponse#errors
            if (!response.getErrors().isEmpty()) {
                // certificateResource.checkX509 only returns a singe error
                ErrorMessage certificateServiceError = response.getErrors().get(0);
                LOG.warn("OCSP check failed {} {}", certificateServiceError.getCode(), certificateServiceError.getText());

                if (certificateServiceError.getCode() == CERT_SERVICE_OCSP_NOT_AVAILABLE_CODE) {
                    throw new AccessKeeperException(ErrorCodes.AUTH_OCSP_ERROR_NO_RESPONSE);
                }
                throw new CertificateServiceException(certificateServiceError.getCode(), certificateServiceError.getText());
            }

            // CERT_REVOKED and CERT_UNKNOWN should also be present in CertificateCheckResponse#errors, but just in case
            // @AFO: A_20318 für nicht existente oder widerrufene Entitäten darf kein AUTH_CODE ausgestellt werden
            if (!ParsedOcspResponse.CertStatusEnum.GOOD.value().equals(response.getParsedOcspResponse().getCertStatus())) {
                // @AFO: A_4829 Bei Fehlerbehandlung Systemmeldungen ausgeben
                LOG.warn("Invalid Cert status {}", response.getParsedOcspResponse().getCertStatus());
                throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_X509_CERT);
            }
        }
        catch (WebApplicationException e) {
            // @AFO: A_4829 Prozess wird beendet, sofern der TUC keine spezifische Fehlerbehandlung beschreibt
            LOG.error("CertificateService request failed");
            throw new AccessKeeperException(ErrorCodes.AUTH_OCSP_ERROR_NO_RESPONSE, e);
        }
        catch (ProcessingException e) {
            // handle timeout when certificate service not available
            if ((e.getCause() instanceof SocketTimeoutException) || (e.getCause() instanceof ConnectException) ||
                (e.getCause() instanceof NoRouteToHostException) || (e.getCause() instanceof UnknownHostException) ||
                 e.getCause() instanceof HttpTimeoutException) {
                LOG.error("CertificateService is not available");
                throw new AccessKeeperException(ErrorCodes.AUTH_OCSP_ERROR_NO_RESPONSE, e);
            }
            else {
                throw e;
            }
        }
    }
}

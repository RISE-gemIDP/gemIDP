/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.service;

import com.rise_world.gematik.accesskeeper.common.dto.CardType;
import com.rise_world.gematik.accesskeeper.common.exception.CertReaderException;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.isismtt.ISISMTTObjectIdentifiers;
import org.bouncycastle.asn1.isismtt.x509.AdmissionSyntax;
import org.bouncycastle.asn1.isismtt.x509.Admissions;
import org.bouncycastle.asn1.isismtt.x509.ProfessionInfo;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.CertificatePolicies;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.PolicyInformation;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.rise_world.gematik.accesskeeper.common.crypt.CryptoConstants.BOUNCY_CASTLE;
import static com.rise_world.gematik.accesskeeper.common.token.ClaimUtils.KVNR_PATTERN;
import static com.rise_world.gematik.accesskeeper.common.token.ClaimUtils.MAX_LENGTH_NAME;
import static com.rise_world.gematik.accesskeeper.common.token.ClaimUtils.MAX_LENGTH_TELEMATIK_ID;

@Component
public class CertificateReaderServiceImpl implements CertificateReaderService {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateReaderServiceImpl.class);

    @Override
    public X509Certificate parseCertificate(String pem) throws CertReaderException {
        try {
            return parseCertificate(Base64.decode(pem));
        }
        catch (CertReaderException e) {
            throw e;
        }
        catch (Exception e) {
            throw new CertReaderException(e);
        }
    }

    @Override
    public X509Certificate parseCertificate(byte[] pem) throws CertReaderException {
        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509", BOUNCY_CASTLE);
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(pem));
        }
        catch (Exception e) {
            throw new CertReaderException(e);
        }
    }

    @Override
    public String extractIdNummer(X509Certificate certificate) throws CertReaderException {
        String idNumber;
        try {
            CardType cardType = getCardType(certificate);

            Map<String, String> cardClaims = new HashMap<>();
            addNameClaims(cardClaims, certificate, cardType);
            addProfessionClaims(cardClaims, certificate, cardType);

            // @AFO: A_21422 - idNummer wird aus dem Zertifikat ausgelesen
            idNumber = cardClaims.get(ClaimUtils.ID_NUMBER);
        }
        catch (CertReaderException e) {
            throw e;
        }
        catch (Exception e) { // catch unexpected exceptions that occured during parsing
            throw new CertReaderException(e);
        }

        if (idNumber == null || StringUtils.length(idNumber) > MAX_LENGTH_TELEMATIK_ID) {
            LOG.warn("'idNummer' is missing or invalid");
            throw new CertReaderException("idNumber is missing or invalid");
        }

        return idNumber;
    }

    @Override
    // @AFO: A_20524-02 - Extrahiert alle ben&ouml;tigten Claims aus dem AUT-Zertifikat
    public Map<String, String> extractCertificateClaims(X509Certificate certificate) throws CertReaderException {
        try {
            return extractCertificateClaimsInternal(certificate);
        }
        catch (CertReaderException e) {
            throw e;
        }
        catch (Exception e) { // catch unexpected exceptions that occured during parsing
            throw new CertReaderException(e);
        }
    }

    private Map<String, String> extractCertificateClaimsInternal(X509Certificate certificate) throws CertReaderException {
        Map<String, String> cardClaims = new HashMap<>();

        CardType cardType = getCardType(certificate);

        // add empty claims
        cardClaims.put(ClaimUtils.FAMILY_NAME, null);
        cardClaims.put(ClaimUtils.GIVEN_NAME, null);
        cardClaims.put(ClaimUtils.DISPLAY_NAME, null);
        cardClaims.put(ClaimUtils.ORG_NAME, null);

        addNameClaims(cardClaims, certificate, cardType);
        addProfessionClaims(cardClaims, certificate, cardType);
        validateClaims(cardClaims, cardType);

        return cardClaims;
    }

    private void validateClaims(Map<String, String> cardClaims, CardType cardType) throws CertReaderException {
        if (cardClaims.get(ClaimUtils.ID_NUMBER) == null) {
            LOG.warn("Required claim 'idNumber' is not available");
            throw new CertReaderException("idNumber is not available");
        }
        if (cardClaims.get(ClaimUtils.PROFESSION) == null) {
            LOG.warn("Required claim 'professionOID' is not available");
            throw new CertReaderException("professionOID is not available");
        }

        if (cardType == CardType.EGK || cardType == CardType.HBA) {
            if (cardClaims.get(ClaimUtils.GIVEN_NAME) == null || cardClaims.get(ClaimUtils.FAMILY_NAME) == null) {
                LOG.warn("{} required claims 'given_name' or 'family_name' not available", cardType);
                throw new CertReaderException("required claims are not available");
            }
        }

        if (cardType == CardType.SMCB && cardClaims.get(ClaimUtils.ORG_NAME) == null) {
            LOG.warn("Required claim 'organizationName' is not available"); // cn is a mandatory field for smcbs
            throw new CertReaderException("required claims are not available");
        }

        if (StringUtils.length(cardClaims.get(ClaimUtils.FAMILY_NAME)) > MAX_LENGTH_NAME ||
            StringUtils.length(cardClaims.get(ClaimUtils.GIVEN_NAME)) > MAX_LENGTH_NAME ||
            StringUtils.length(cardClaims.get(ClaimUtils.ORG_NAME)) > MAX_LENGTH_NAME ||
            StringUtils.length(cardClaims.get(ClaimUtils.ID_NUMBER)) > MAX_LENGTH_TELEMATIK_ID) {
            LOG.warn("claim length validation failed");
            throw new CertReaderException("Claim length validation failed");
        }
    }

    @Override
    public CardType getCardType(X509Certificate x509Certificate) throws CertReaderException {
        Set<String> policyIds = extractPolicyIds(x509Certificate);

        if (policyIds.contains(OidType.OID_C_CH_AUTH)) {
            return CardType.EGK;
        }
        else if (policyIds.contains(OidType.OID_C_HP_AUTH)) {
            return CardType.HBA;
        }
        else if (policyIds.contains(OidType.OID_C_HCI_AUTH)) {
            return CardType.SMCB;
        }

        LOG.warn("unexpected card type");
        throw new CertReaderException("Unexpected card type");
    }

    private void addNameClaims(Map<String, String> cardClaims, X509Certificate certificate, CardType cardType) {
        // extract name fields
        X500Principal principal = certificate.getSubjectX500Principal();
        X500Name x500name = new X500Name(principal.getName());
        RDN[] nameRDNs = x500name.getRDNs();

        for (RDN rdn : nameRDNs) {
            for (AttributeTypeAndValue attribute : rdn.getTypesAndValues()) {
                String value = extractValue(attribute.getValue());

                if (BCStyle.GIVENNAME.equals(attribute.getType())) {
                    cardClaims.put(ClaimUtils.GIVEN_NAME, value);
                }
                else if (BCStyle.SURNAME.equals(attribute.getType())) {
                    cardClaims.put(ClaimUtils.FAMILY_NAME, value);
                }
                else if (cardType == CardType.EGK && X509ObjectIdentifiers.organization.equals((attribute.getType()))) {
                    cardClaims.put(ClaimUtils.ORG_NAME, value);
                }
                else if (cardType == CardType.EGK && BCStyle.OU.equals(attribute.getType()) && value != null && KVNR_PATTERN.matcher(value).matches()) {
                    cardClaims.put(ClaimUtils.ID_NUMBER, value);
                }
                else if (cardType == CardType.SMCB && BCStyle.CN.equals(attribute.getType())) {
                    cardClaims.put(ClaimUtils.ORG_NAME, value);
                }
            }
        }

        if (cardClaims.get(ClaimUtils.GIVEN_NAME) != null && cardClaims.get(ClaimUtils.FAMILY_NAME) != null) {
            cardClaims.put(ClaimUtils.DISPLAY_NAME, cardClaims.get(ClaimUtils.GIVEN_NAME) + " " + cardClaims.get(ClaimUtils.FAMILY_NAME));
        }
    }

    private String extractValue(ASN1Encodable encodable) {
        if (encodable == null) {
            return null;
        }
        else if (encodable instanceof ASN1String s) {
            return s.getString();
        }
        else {
            LOG.warn("unexpected asn1 value type: {}", encodable.getClass().getName());
            return IETFUtils.valueToString(encodable);
        }
    }

    private void addProfessionClaims(Map<String, String> cardClaims, X509Certificate certificate, CardType cardType) throws CertReaderException {
        for (ProfessionInfo professionInfo : extractProfessionInfos(certificate)) {
            if (professionInfo.getRegistrationNumber() != null && (cardType == CardType.HBA || cardType == CardType.SMCB)) {
                cardClaims.put(ClaimUtils.ID_NUMBER, professionInfo.getRegistrationNumber());
            }
            // gemspec_idp defines profession as single valued, therefore in case of multiple professionOids the last one 'wins'
            for (ASN1ObjectIdentifier professionOID : professionInfo.getProfessionOIDs()) {
                cardClaims.put(ClaimUtils.PROFESSION, professionOID.getId());
            }
        }
    }

    private Set<String> extractPolicyIds(X509Certificate x509Certificate) throws CertReaderException {
        byte[] policyExtension = x509Certificate.getExtensionValue(Extension.certificatePolicies.getId());
        if (policyExtension == null) {
            LOG.warn("policy extension is missing");
            throw new CertReaderException("Missing policy extension");
        }

        PolicyInformation[] policyInformation;
        try {
            ASN1Primitive asn1Primitive = JcaX509ExtensionUtils.parseExtensionValue(policyExtension);
            CertificatePolicies policies = CertificatePolicies.getInstance(asn1Primitive);
            policyInformation = policies.getPolicyInformation();
        }
        catch (Exception e) {
            LOG.warn("failed to parse policy extension");
            throw new CertReaderException("Failed to parse policy information", e);
        }

        Set<String> policyIds = new HashSet<>();
        for (PolicyInformation info : policyInformation) {
            policyIds.add(info.getPolicyIdentifier().getId());
        }
        return policyIds;
    }

    private ProfessionInfo[] extractProfessionInfos(X509Certificate x509Certificate) throws CertReaderException {
        byte[] admissionExtension = x509Certificate.getExtensionValue(ISISMTTObjectIdentifiers.id_isismtt_at_admission.getId());
        if (admissionExtension == null) {
            return new ProfessionInfo[0];
        }

        AdmissionSyntax admissionSyntax;
        try {
            ASN1Primitive asn1Primitive = JcaX509ExtensionUtils.parseExtensionValue(admissionExtension);
            admissionSyntax = AdmissionSyntax.getInstance(asn1Primitive);
        }
        catch (IOException e) {
            LOG.warn("failed to parse admission");
            throw new CertReaderException("Failed to parse admission", e);
        }

        // Tab_PKI_226-01 gemspec_pki: "Diese Sequenz MUSS genau ein Element vom Typ Admissions enthalten."
        if (admissionSyntax.getContentsOfAdmissions().length > 0) {
            Admissions admissions = admissionSyntax.getContentsOfAdmissions()[0];
            return admissions.getProfessionInfos();
        }
        else {
            return new ProfessionInfo[0];
        }
    }
}

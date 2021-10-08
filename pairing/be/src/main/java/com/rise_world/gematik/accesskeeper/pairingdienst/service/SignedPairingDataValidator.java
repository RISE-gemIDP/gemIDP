/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rise_world.gematik.accesskeeper.common.crypt.CryptoConstants;
import com.rise_world.gematik.accesskeeper.common.dto.CardType;
import com.rise_world.gematik.accesskeeper.common.exception.CertReaderException;
import com.rise_world.gematik.accesskeeper.common.service.CertificateReaderService;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import com.rise_world.gematik.accesskeeper.pairingdienst.Constants;
import com.rise_world.gematik.accesskeeper.pairingdienst.dto.DeviceTypeDTO;
import com.rise_world.gematik.accesskeeper.pairingdienst.dto.SignedPairingDataDTO;
import com.rise_world.gematik.accesskeeper.pairingdienst.service.exception.InvalidSignedPairingDataException;
import com.rise_world.gematik.accesskeeper.pairingdienst.service.validation.Validations;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.EcDsaJwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Objects;

import static com.rise_world.gematik.accesskeeper.pairingdienst.token.PairingClaims.PAIRING_DATA_CLAIM_SE_SUBJECT_PUBLIC_KEY_INFO;
import static com.rise_world.gematik.accesskeeper.pairingdienst.util.Utils.BASE64URL_DECODER;
import static com.rise_world.gematik.accesskeeper.pairingdienst.util.Utils.BASE64URL_ENCODER;
import static com.rise_world.gematik.accesskeeper.pairingdienst.util.Utils.timeConstantNotEquals;

/**
 * Validates a Signed_Pairing_Data structure.
 */
@Component
public class SignedPairingDataValidator {

    private static final Logger LOG = LoggerFactory.getLogger(SignedPairingDataValidator.class);

    private static final ECNamedCurveParameterSpec BC_EC_PARAM_SPEC_SEC_P256_R1 = ECNamedCurveTable.getParameterSpec(CryptoConstants.CURVE_SEC_P256_R1);
    private static final EllipticCurve EC_CURVE_SEC_P256_R1 = EC5Util.convertCurve(
        BC_EC_PARAM_SPEC_SEC_P256_R1.getCurve(), BC_EC_PARAM_SPEC_SEC_P256_R1.getSeed());
    private static final ECParameterSpec EC_PARAM_SPEC_SEC_P256_R1 = EC5Util.convertSpec(EC_CURVE_SEC_P256_R1, BC_EC_PARAM_SPEC_SEC_P256_R1);

    private CertificateReaderService certificateReaderService;
    private ObjectMapper objectMapper;

    @Autowired
    public SignedPairingDataValidator(CertificateReaderService certificateReaderService, ObjectMapper objectMapper) {
        this.certificateReaderService = certificateReaderService;
        this.objectMapper = objectMapper;
    }

    /**
     * Validate a Signed_Pairing_Data structure.
     * <p>
     * This method performs all checks on signed pairing data that are needed during a pairing registration.
     *
     * @param signedPairingDataAsString signed pairing data to be validated
     * @param authCertificate           authCertificate (i.e. "Authentisierungszertifikat der eGK")
     * @param product the product name of the device (see {@link DeviceTypeDTO#getProduct()})
     * @return keyIdentifier extracted from signed pairing data
     */
    public String validateForRegistration(String signedPairingDataAsString, AuthCertificate authCertificate, String product) {
        IdpJwsJwtCompactConsumer signedPairingData = new IdpJwsJwtCompactConsumer(signedPairingDataAsString);

        validateHeaders(signedPairingData.getJwsHeaders());
        validateSignature(signedPairingData, authCertificate.asX509Certificate().getPublicKey());

        SignedPairingDataDTO pairingDataClaims;
        try {
            pairingDataClaims = this.objectMapper.readValue(signedPairingData.getDecodedJwsPayload(), SignedPairingDataDTO.class);
        }
        catch (Exception e) {
            throw new InvalidSignedPairingDataException("format of registration_data.signed_pairing_data cannot be read", e);
        }
        return validateRegistrationClaims(pairingDataClaims, authCertificate, product);
    }

    /**
     * Validate a Signed_Pairing_Data structure.
     * <p>
     * This method performs all checks on signed pairing data that are needed during an authentication.
     *
     * @param signedPairingDataAsString signed pairing data to be validated
     * @param authCertificate           authCertificate (i.e. "Authentisierungszertifikat der eGK")
     * @return PUK_SE_AUT extracted from signed pairing data
     */
    public ECPublicKey validateForAuthentication(String signedPairingDataAsString, AuthCertificate authCertificate) {
        IdpJwsJwtCompactConsumer signedPairingData = new IdpJwsJwtCompactConsumer(signedPairingDataAsString);
        validateSignature(signedPairingData, authCertificate.asX509Certificate().getPublicKey());

        JwtClaims pairingDataClaims = signedPairingData.getJwtClaims();

        // construct ECPublicKey for PUK_SE_AUT (pairingDataKeyData contains the public key PUK_SE_AUT)
        final SubjectPublicKeyInfo pairingDataKeyData =
            SubjectPublicKeyInfo.getInstance(BASE64URL_DECODER.decode(pairingDataClaims.getStringProperty(PAIRING_DATA_CLAIM_SE_SUBJECT_PUBLIC_KEY_INFO)));
        byte[] publicKeyPointAsBytes = pairingDataKeyData.getPublicKeyData().getBytes();

        try {
            ECPoint ecPoint = ECPointUtil.decodePoint(EC_CURVE_SEC_P256_R1, publicKeyPointAsBytes);
            ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec(ecPoint, EC_PARAM_SPEC_SEC_P256_R1);

            KeyFactory kf = KeyFactory.getInstance("EC", CryptoConstants.BOUNCY_CASTLE);
            return (ECPublicKey) kf.generatePublic(ecPublicKeySpec);
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException e) {
            LOG.error("failed to construct PUK_SE_AUT from pairing data");
            throw new InvalidSignedPairingDataException(e);
        }
    }

    private void validateHeaders(JwsHeaders jwsHeaders) {
        if (!ClaimUtils.hasJoseType(jwsHeaders, JoseType.JWT)) {
            LOG.warn("invalid signed pairing data: type {}", jwsHeaders.getHeader(JoseConstants.HEADER_TYPE));
            throw new InvalidSignedPairingDataException("invalid token type");
        }

        if (jwsHeaders.getAlgorithm() == null) {
            LOG.warn("invalid signed pairing data: missing alg header");
            throw new InvalidSignedPairingDataException("missing algorithm header");
        }

        if (!CryptoConstants.SIG_ALG_BRAINPOOL_P256_R1.equals(jwsHeaders.getAlgorithm())) {
            LOG.warn("unsupported signing algorithm: {}", jwsHeaders.getAlgorithm());
            throw new InvalidSignedPairingDataException("invalid algorithm header");
        }
    }

    // @AFO: A_21421 - Die Signatur des Pairingdaten wird mit dem &uuml;bermittelten AUT-Zertifikat nachgerechnet
    // @AFO: A_21435 - Die Signatur des Pairingdaten wird mit dem &uuml;bermittelten AUT-Zertifikat nachgerechnet
    private void validateSignature(IdpJwsJwtCompactConsumer signedPairingData, PublicKey authCertificatePublicKey) {
        if (StringUtils.isEmpty(signedPairingData.getEncodedSignature())) {
            LOG.warn("missing signature in signed pairing data");
            throw new InvalidSignedPairingDataException("missing signature");
        }

        JwsHeaders headers = signedPairingData.getJwsHeaders();
        String algorithm = headers.getAlgorithm();

        JwsSignatureVerifier jwsSignatureVerifier;

        if (CryptoConstants.SIG_ALG_BRAINPOOL_P256_R1.equals(algorithm)) {
            jwsSignatureVerifier = new EcDsaJwsSignatureVerifier(authCertificatePublicKey, SignatureAlgorithm.ES256);
        }
        else {
            LOG.warn("unsupported signature algorithm: {}", algorithm);
            throw new InvalidSignedPairingDataException("unsupported signature algorithm");
        }

        if (!signedPairingData.verifySignatureWith(jwsSignatureVerifier)) {
            throw new InvalidSignedPairingDataException("invalid signature");
        }
    }

    private String validateRegistrationClaims(SignedPairingDataDTO pairingDataClaims, AuthCertificate authCertificate, String product) {

        if (!pairingDataClaims.isValid()) {
            LOG.warn("missing required claims in registration_data.signed_pairing_data");
            throw new InvalidSignedPairingDataException("required claims in registration_data.signed_pairing_data");
        }

        if (!SignedPairingDataDTO.EXPECTED_VERSION.isValid(pairingDataClaims.getPairingDataVersion())) {
            LOG.warn("invalid signed pairing data: version {}", pairingDataClaims.getPairingDataVersion());
            throw new InvalidSignedPairingDataException("invalid version");
        }

        if (!Validations.KEY_IDENTIFIER.isValid(pairingDataClaims.getKeyIdentifier())) {
            LOG.warn("invalid signed pairing data: invalid keyIdentifier");
            LOG.debug("keyIdentifier={}", pairingDataClaims.getKeyIdentifier());
            throw new InvalidSignedPairingDataException("key_identifier is invalid");
        }

        if (!Objects.equals(pairingDataClaims.getProduct(), product)) {
            LOG.warn("invalid signed pairing data: product claim doesn't match product name in device information");
            throw new InvalidSignedPairingDataException("invalid product");
        }

        validateCertificateClaims(pairingDataClaims, authCertificate);

        // validate public key PUK_SE_AUT of device (in pairing_data.se_subject_public_key_info)
        final SubjectPublicKeyInfo claimPairingDataKeyData =
            SubjectPublicKeyInfo.getInstance(BASE64URL_DECODER.decode(pairingDataClaims.getSePublicKeyInfo()));
        // @AFO: A_21439 - die Kurve des PublicKeys des Secure Elements muss P-256 sein
        validatePublicKey(Constants.ALLOWED_EC_NAMED_CURVES_DEVICE_SECURE_ELEMENT, claimPairingDataKeyData);

        return pairingDataClaims.getKeyIdentifier();
    }

    private void validateCertificateClaims(SignedPairingDataDTO pairingDataClaims, AuthCertificate authCertificate) {
        try {
            X509Certificate x509Certificate = authCertificate.asX509Certificate();

            validateCardType(x509Certificate);

            // base64url-encoded, DER-encoded, ASN.1 structure 'subjectPublicKeyInfo' according to RFC5280#section-4.1.2.7
            Certificate bcAuthCertificate = Certificate.getInstance(authCertificate.asBytes());
            X509CertificateHolder x509CertificateHolder = new X509CertificateHolder(bcAuthCertificate);
            byte[] claimPairingDataPublicKey =
                BASE64URL_DECODER.decode(pairingDataClaims.getPukCertificateInfo());
            SubjectPublicKeyInfo subjectPublicKeyInfo = x509CertificateHolder.getSubjectPublicKeyInfo();
            byte[] certSubjectPublicKeyInfo = subjectPublicKeyInfo.getEncoded();
            // @AFO: A_21421 - das AUT Zertifikat muss vom Typ ECC sein
            validatePublicKey(Constants.ALLOWED_EC_NAMED_CURVES_AUTH_CARD, subjectPublicKeyInfo);

            // @AFO: A_21470 - PublicKey muss ident sein
            if (timeConstantNotEquals(claimPairingDataPublicKey, certSubjectPublicKeyInfo)) {
                LOG.warn("mismatch of subject public key info");
                throw new InvalidSignedPairingDataException("C.CH.AUT public key doesn't match claim");
            }

            // base64url-encoded, DER-encoded, ASN.1 structure 'issuer' according to RFC5280#section-4.1.2.4
            byte[] claimIssuer = BASE64URL_DECODER.decode(pairingDataClaims.getIssuer());
            byte[] certIssuer = x509Certificate.getIssuerX500Principal().getEncoded();

            // @AFO: A_21470 - Issuer muss ident sein
            if (timeConstantNotEquals(claimIssuer, certIssuer)) {
                LOG.warn("mismatch of issuer");
                LOG.debug("claimIssuer={}, certIssuer={}", BASE64URL_ENCODER.encode(claimIssuer), BASE64URL_ENCODER.encode(certIssuer));
                throw new InvalidSignedPairingDataException("issuer doesn't match");
            }

            BigInteger claimSerialnumber = new BigInteger(pairingDataClaims.getSerialnumber(), 10);
            BigInteger certSerialnumber = x509Certificate.getSerialNumber();

            // @AFO: A_21470 - Seriennummer muss ident sein
            if (!claimSerialnumber.equals(certSerialnumber)) {
                LOG.warn("mismatch of serialnumber");
                LOG.debug("claimSerialnumber={}, certSerialnumber={}", claimSerialnumber, certSerialnumber);
                throw new InvalidSignedPairingDataException("serialnumber doesn't match");
            }

            Long claimNotAfterEpochSecond = pairingDataClaims.getNotAfter();
            long certNotAfterEpochSecond = x509Certificate.getNotAfter().getTime() / 1000L;

            // @AFO: A_21470 - notAfter muss ident sein
            if (!Objects.equals(claimNotAfterEpochSecond, certNotAfterEpochSecond)) {
                LOG.warn("mismatch of notAfter");
                LOG.debug("claimNotAfterEpochSecond={}, certNotAfterEpochSecond={}", claimNotAfterEpochSecond, certNotAfterEpochSecond);
                throw new InvalidSignedPairingDataException("notAfter doesn't match");
            }
        }
        catch (IOException e) {
            LOG.warn("validation of pairing data failed");
            throw new InvalidSignedPairingDataException(e);
        }
    }

    private void validateCardType(X509Certificate x509Certificate) {
        try {
            if (certificateReaderService.getCardType(x509Certificate) != CardType.EGK) {
                LOG.warn("no C.CH.AUT of an eGK presented, therefore unsupported card type");
                throw new InvalidSignedPairingDataException("invalid card type");
            }
        }
        catch (CertReaderException e) {
            LOG.warn("failed to get card type");
            throw new InvalidSignedPairingDataException(e);
        }
    }

    private void validatePublicKey(List<ASN1ObjectIdentifier> allowedEcNamedCurves, SubjectPublicKeyInfo pubKeyInfo) {
        final AlgorithmIdentifier algId = pubKeyInfo.getAlgorithm();
        final ASN1ObjectIdentifier algorithmObjectId = algId.getAlgorithm();

        // ECC PublicKey
        if (X9ObjectIdentifiers.id_ecPublicKey.equals(algorithmObjectId)) {
            // validate named curve of contained public key
            final ASN1ObjectIdentifier namedCurve = ASN1ObjectIdentifier.getInstance(algId.getParameters());
            if (!allowedEcNamedCurves.contains(namedCurve)) {
                LOG.warn("presented certificate contains public key with unsupported named curve with algorithm identifier {}", namedCurve);
                throw new InvalidSignedPairingDataException("invalid ec curve");
            }

            // validate public key exists in uncompressed format
            final ECNamedCurveParameterSpec namedCurveParameterSpec = ECNamedCurveTable.getParameterSpec(namedCurve.getId());
            final byte[] pubKeyPointBytes = pubKeyInfo.getPublicKeyData().getBytes();
            if (pubKeyPointBytes[0] != (byte) 0x04) {
                LOG.warn("EC public point format of public key IS NOT uncompressed");
                LOG.debug("EC public point leading byte {}", pubKeyPointBytes[0]);
                throw new InvalidSignedPairingDataException("EC public point is compressed");
            }

            // validate public key point on curve (the subsequent BouncyCastle call implies point validation)
            try {
                namedCurveParameterSpec.getCurve().decodePoint(pubKeyPointBytes);
            }
            catch (Exception e) {
                LOG.warn("EC public key validation failed");
                throw new InvalidSignedPairingDataException("EC public key validation failed", e);
            }
        }
        // unsupported key type
        else {
            LOG.warn("unsupported public key type");
            throw new InvalidSignedPairingDataException("unsupported public key type");
        }

        LOG.debug("EC public key validated successfully");
    }
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.crypt.kms;

import com.rise_world.gematik.accesskeeper.common.crypt.DecryptionProviderFactory;
import com.rise_world.gematik.accesskeeper.common.crypt.EncryptionProviderFactory;
import com.rise_world.gematik.accesskeeper.common.crypt.IdNummerAnonymizer;
import com.rise_world.gematik.accesskeeper.common.crypt.KeyConstants;
import com.rise_world.gematik.accesskeeper.common.crypt.KeyProvider;
import com.rise_world.gematik.accesskeeper.common.crypt.SignatureProviderFactory;
import com.rise_world.gematik.accesskeeper.common.dto.Endpoint;
import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
import com.rise_world.gematik.idp.kms.api.rest.CertificateResource;
import com.rise_world.gematik.idp.kms.api.rest.PairingResource;
import com.rise_world.gematik.idp.kms.api.rest.TokenResource;
import com.rise_world.gematik.idp.kms.api.rest.model.JwkEcPublicKey;
import com.rise_world.gematik.idp.kms.api.rest.model.KeyType;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.AesGcmContentDecryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweDecryption;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Base64;

import static com.rise_world.gematik.accesskeeper.common.crypt.CryptoConstants.BOUNCY_CASTLE;

@Component
public class KMSProviderFactory implements EncryptionProviderFactory, DecryptionProviderFactory, SignatureProviderFactory, KeyProvider, IdNummerAnonymizer {

    private static final Logger LOG = LoggerFactory.getLogger(KMSProviderFactory.class);

    private static final Base64.Encoder BASE64URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private TokenResource tokenResource;
    private CertificateResource certResource;
    private PairingResource pairingResource;

    @Autowired
    public KMSProviderFactory(TokenResource tokenResource, CertificateResource certResource, PairingResource pairingResource) {
        this.tokenResource = tokenResource;
        this.certResource = certResource;
        this.pairingResource = pairingResource;

        LOG.info("KMSProviderFactory was initialized");
    }

    @Override
    public JwsSignatureProvider createSignatureProvider(Endpoint endpoint) {
        return new KMSSignatureProvider(this.tokenResource, toKeyType(endpoint));
    }

    @Override
    public JweDecryptionProvider createDecryptionProvider(TokenType type) {
        if (type.isDirectDecrypt()) {
            // AES
            return new KMSDirectDecryption(tokenResource, toTokenType(type));
        }
        // @AFO: A_21420 - Registrierungsdaten werden mit ECDH_ES_DIRECT entschl&uuml;sselt
        // @AFO: A_21445 - AccessToken wird mit ECDH_ES_DIRECT entschl&uuml;sselt
        else if (type.isEcdhEsDecrypt()) {
            // ECDH
            return new JweDecryption(new KMSEcdhDirectKeyDecryptionAlgorithm(tokenResource), new AesGcmContentDecryptionAlgorithm(ContentAlgorithm.A256GCM));
        }

        throw new IllegalArgumentException();
    }

    private com.rise_world.gematik.idp.kms.api.rest.model.TokenType toTokenType(TokenType type) {
        if (TokenType.SSO == type) {
            return com.rise_world.gematik.idp.kms.api.rest.model.TokenType.SSO_TOKEN;
        }
        else if (TokenType.AUTH_CODE == type) {
            return com.rise_world.gematik.idp.kms.api.rest.model.TokenType.AUTH_CODE;
        }

        return null;
    }

    @Override
    public JweEncryptionProvider createEncryptionProvider(TokenType type) {
        if (TokenType.SSO == type || TokenType.AUTH_CODE == type) {
            return new KMSDirectEncryption(tokenResource, toTokenType(type));
        }

        throw new IllegalArgumentException();
    }

    @Override
    public ECPublicKey getKey(String kid) {
        if (KeyConstants.PUK_IDP_ENC.equals(kid)) {
            JwkEcPublicKey keyDefinition = certResource.getPublicKey();

            return createECKey(keyDefinition.getCrv(), keyDefinition.getX(), keyDefinition.getY());
        }

        throw new IllegalArgumentException(String.format("KeyIdentifier %s is invalid", kid));
    }

    private ECPublicKey createECKey(String curve, String encodedXPoint, String encodedYPoint) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", BOUNCY_CASTLE);
            ECGenParameterSpec params = new ECGenParameterSpec(curve);
            kpg.initialize(params);
            KeyPair pair = kpg.generateKeyPair();
            ECParameterSpec pubParam = ((ECPublicKey) pair.getPublic()).getParams();

            byte[] decodedX = CryptoUtils.decodeSequence(encodedXPoint);
            byte[] decodedY = CryptoUtils.decodeSequence(encodedYPoint);

            ECPoint ecPoint = new ECPoint(new BigInteger(1, decodedX), new BigInteger(1, decodedY));
            ECPublicKeySpec keySpec = new ECPublicKeySpec(ecPoint, pubParam);
            KeyFactory kf = KeyFactory.getInstance("EC", BOUNCY_CASTLE);
            return (ECPublicKey) kf.generatePublic(keySpec);
        }
        catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }

    @Override
    public X509Certificate getCertificate(String kid) {
        try {
            byte[] latestCertificate = certResource.getLatestCertificate(toKeyType(kid));
            return (X509Certificate) CertificateFactory.getInstance("X.509", BOUNCY_CASTLE).generateCertificate(new ByteArrayInputStream(latestCertificate));
        }
        catch (CertificateException | NoSuchProviderException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private KeyType toKeyType(Endpoint endpoint) {
        if (endpoint == null) {
            throw new IllegalArgumentException();
        }

        switch (endpoint) {
            case AUTH:
            case TOKEN:
                return KeyType.IDP;
            case DISC:
                return KeyType.DISC;
            default:
                throw new IllegalArgumentException();
        }
    }

    private KeyType toKeyType(String kid) {
        switch (kid) {
            case KeyConstants.PUK_IDP_SIG:
                return KeyType.IDP;
            case KeyConstants.PUK_DISC_SIG:
                return KeyType.DISC;
            default:
                throw new IllegalArgumentException(String.format("KeyIdentifier %s is invalid", kid));
        }
    }

    @Override
    public String anonymizeIdNummer(String idNummer) {
        return BASE64URL_ENCODER.encodeToString(pairingResource.anonymizeIdNummer(idNummer));
    }
}

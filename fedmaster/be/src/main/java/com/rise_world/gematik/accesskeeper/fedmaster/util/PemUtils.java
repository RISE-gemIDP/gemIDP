/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.util;

import com.rise_world.gematik.accesskeeper.common.util.DigestUtils;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PemUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PemUtils.class);

    private static final Map<ASN1ObjectIdentifier, String> ALGORITHMS = new HashMap<>();

    // org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter maps id_ecPublicKey to 'ECDSA'. this doesn't work with cxf JwsUtils beacuse they expect 'EC'
    static {
        ALGORITHMS.put(X9ObjectIdentifiers.id_ecPublicKey, "EC");
        ALGORITHMS.put(PKCSObjectIdentifiers.rsaEncryption, "RSA");
        ALGORITHMS.put(X9ObjectIdentifiers.id_dsa, "DSA");
    }

    private PemUtils() {
        // avoid instantiation
    }

    public static Optional<PublicKey> readPublicKey(String pem) {
        PEMParser parser = new PEMParser(new StringReader(pem));
        try {
            Object pemObject = parser.readObject();
            if (pemObject instanceof SubjectPublicKeyInfo pubKey) {
                KeyFactory keyFactory = getKeyFactory(pubKey.getAlgorithm());
                PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(pubKey.getEncoded()));
                return Optional.of(publicKey);
            }
            LOG.info("expected a SubjectPublicKeyInfo but found {}", pemObject);
        }
        catch (Exception e) {
            LOG.error("problem reading PEM formatted string", e);
            return Optional.empty();
        }

        return Optional.empty();
    }

    private static KeyFactory getKeyFactory(AlgorithmIdentifier algId) throws NoSuchAlgorithmException, NoSuchProviderException {
        ASN1ObjectIdentifier algorithm =  algId.getAlgorithm();

        String algName = ALGORITHMS.get(algorithm);

        if (algName == null) {
            algName = algorithm.getId();
        }

        return KeyFactory.getInstance(algName, BouncyCastleProvider.PROVIDER_NAME);
    }

    public static String hashKey(String pem) {
        try {
            PEMParser parser = new PEMParser(new StringReader(pem));
            byte[] key = ((ASN1Object) parser.readObject()).getEncoded(ASN1Encoding.DER);
            return Hex.toHexString(DigestUtils.sha256(key));
        }
        catch (Exception e) {
            LOG.error("problem reading PEM formatted string", e);
            return null;
        }
    }
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.util;

import com.rise_world.gematik.accesskeeper.common.util.DigestUtils;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.security.PublicKey;
import java.util.Optional;

public class PemUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PemUtils.class);

    private PemUtils() {
        // avoid instantiation
    }

    public static Optional<PublicKey> readPublicKey(String pem) {
        PEMParser parser = new PEMParser(new StringReader(pem));
        try {
            Object pemObject = parser.readObject();
            if (pemObject instanceof SubjectPublicKeyInfo pubKey) {
                return Optional.of(new JcaPEMKeyConverter().getPublicKey(pubKey));
            }
            LOG.info("expected a SubjectPublicKeyInfo but found {}", pemObject);
        }
        catch (IOException e) {
            LOG.error("problem reading PEM formatted string", e);
            return Optional.empty();
        }

        return Optional.empty();
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

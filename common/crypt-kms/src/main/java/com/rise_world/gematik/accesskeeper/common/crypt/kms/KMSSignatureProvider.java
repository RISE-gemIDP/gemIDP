/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.crypt.kms;

import com.rise_world.gematik.idp.kms.api.rest.TokenResource;
import com.rise_world.gematik.idp.kms.api.rest.model.KeyType;
import org.apache.cxf.rs.security.jose.common.JoseException;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignature;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;

import java.util.Arrays;

public class KMSSignatureProvider implements JwsSignatureProvider {

    private static final int SIGNATURE_LENGTH_ES256 = 64;
    private TokenResource tokenResource;
    private KeyType keyType;

    public KMSSignatureProvider(TokenResource kmsTokenResource, KeyType keyType) {
        this.tokenResource = kmsTokenResource;
        this.keyType = keyType;
    }

    @Override
    public JwsSignature createJwsSignature(JwsHeaders headers) {
        return new KMSPrivateKeyJwsSignature(tokenResource, keyType);
    }

    @Override
    public SignatureAlgorithm getAlgorithm() {
        // @AFO: A_23034 ES256 wird verwendet
        return SignatureAlgorithm.ES256;
    }

    @Override
    public byte[] sign(JwsHeaders headers, byte[] content) {
        JwsSignature sig = createJwsSignature(headers);
        sig.update(content, 0, content.length);
        return sig.sign();
    }

    protected static class KMSPrivateKeyJwsSignature implements JwsSignature {

        private TokenResource endpoint;
        private KeyType keyType;
        private byte[] payload = new byte[] {};

        public KMSPrivateKeyJwsSignature(TokenResource endpoint, KeyType type) {
            this.endpoint = endpoint;
            this.keyType = type;
        }

        //@AFO: A_20521-02 - Signiert Payload mittels Schlüsselinformation am HSM
        @Override
        public byte[] sign() {
            try {
                byte[] jcaDer = this.endpoint.sign(payload, keyType);
                return KMSPrivateKeyJwsSignature.jcaOutputToJoseOutput(SIGNATURE_LENGTH_ES256, jcaDer);
            }
            finally {
                // erase payload
                Arrays.fill(payload, (byte) 0);
                payload = new byte[] {};
            }
        }

        @Override
        public void update(byte[] src, int off, int len) {
            if (payload.length < off + len) {
                payload = Arrays.copyOf(payload, off + len);
            }
            System.arraycopy(src, 0, payload, off, len);
        }

        // CHECKSTYLE:OFF
        // copied as is from EcDsaJwsSignatureProvider
        @SuppressWarnings("squid:S1192")
        private static byte[] jcaOutputToJoseOutput(int jwsSignatureLen, byte[] jcaDer) {
            // Apache2 Licensed Jose4j code which adapts the Apache Santuario XMLSecurity
            // code and aligns it with JWS/JWA requirements
            if (jcaDer.length < 8 || jcaDer[0] != 48) {
                throw new JoseException("Invalid format of ECDSA signature");
            }

            int offset;
            if (jcaDer[1] > 0) {
                offset = 2;
            }
            else if (jcaDer[1] == (byte) 0x81) {
                offset = 3;
            }
            else {
                throw new JoseException("Invalid format of ECDSA signature");
            }

            byte rLength = jcaDer[offset + 1];

            int i;
            for (i = rLength; i > 0 && jcaDer[(offset + 2 + rLength) - i] == 0; i--) {
                // complete
            }

            byte sLength = jcaDer[offset + 2 + rLength + 1];

            int j;
            for (j = sLength; j > 0 && jcaDer[(offset + 2 + rLength + 2 + sLength) - j] == 0; j--) {
                // complete
            }

            int rawLen = Math.max(i, j);
            rawLen = Math.max(rawLen, jwsSignatureLen / 2);

            if ((jcaDer[offset - 1] & 0xff) != jcaDer.length - offset || (jcaDer[offset - 1] & 0xff) != 2 + rLength + 2 + sLength || jcaDer[offset] != 2
                    || jcaDer[offset + 2 + rLength] != 2) {
                throw new JoseException("Invalid format of ECDSA signature");
            }

            byte[] concatenatedSignatureBytes = new byte[2 * rawLen];

            System.arraycopy(jcaDer, (offset + 2 + rLength) - i, concatenatedSignatureBytes, rawLen - i, i);
            System.arraycopy(jcaDer, (offset + 2 + rLength + 2 + sLength) - j, concatenatedSignatureBytes, 2 * rawLen - j, j);

            return concatenatedSignatureBytes;
        }
        // CHECKSTYLE:ON
    }

}

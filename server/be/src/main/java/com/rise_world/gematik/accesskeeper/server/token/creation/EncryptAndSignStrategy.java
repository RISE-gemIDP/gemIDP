/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.creation;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.common.util.JwtJsonUtils;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweCompactProducer;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweException;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;

import java.util.Collections;
import java.util.Objects;

public class EncryptAndSignStrategy extends SignStrategy {

    private JweEncryptionProvider encryption = null;

    public EncryptAndSignStrategy(JweEncryptionProvider provider, JwsSignatureProvider signer, String kid) {
        this(provider, signer, kid, JoseType.JWT.toString());
    }

    public EncryptAndSignStrategy(JweEncryptionProvider provider, JwsSignatureProvider signer, String kid, String type) {
        super(signer, kid, type);
        this.encryption = Objects.requireNonNull(provider);
    }

    @Override
    public String toToken(JwtClaims claims) {
        String signed = super.toToken(claims);

        try {
            JweHeaders headers = createJWEHeader();
            // add exp claim to header. this way decrypt can be avoided for expired tokens
            if (claims.containsProperty(JwtConstants.CLAIM_EXPIRY)) {
                headers.setHeader(JwtConstants.CLAIM_EXPIRY, claims.getExpiryTime());
            }
            String payload = JwtJsonUtils.serializeClaims(new JwtClaims(Collections.singletonMap(ClaimUtils.NESTED_TOKEN, signed)));
            JweCompactProducer jweProducer = new JweCompactProducer(headers, payload);

            // @AFO: GS-A_5016 - Symmetrische Verschlüsselung binärer Daten
            return jweProducer.encryptWith(encryption);
        }
        catch (JweException ex) {
            throw new AccessKeeperException(ErrorCodes.AUTH_ENCRYPTION, ex);
        }
    }

    private JweHeaders createJWEHeader() {
        JweHeaders headers = new JweHeaders();
        headers.setContentEncryptionAlgorithm(ContentAlgorithm.A256GCM);
        headers.setContentType(ClaimUtils.NESTED_TOKEN_CTY_VALUE);
        return headers;
    }

}

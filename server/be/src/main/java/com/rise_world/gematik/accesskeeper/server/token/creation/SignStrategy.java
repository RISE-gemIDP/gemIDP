/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.creation;

import com.rise_world.gematik.accesskeeper.common.crypt.CryptoConstants;
import com.rise_world.gematik.accesskeeper.common.token.creation.IdpJwsHeaders;
import com.rise_world.gematik.accesskeeper.common.util.JwtJsonUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;

import java.util.Collections;
import java.util.function.Supplier;

public class SignStrategy implements TokenCreationStrategy {

    private JwsSignatureProvider signer;
    private String kid;
    private String type;
    private Supplier<String> certSupplier;

    public SignStrategy(JwsSignatureProvider signer, String kid) {
        this(signer, kid, JoseType.JWT.toString());
    }
    public SignStrategy(JwsSignatureProvider signer, String kid, String type) {
        this(signer, kid, type, null);
    }

    public SignStrategy(JwsSignatureProvider signer, String kid, String type, Supplier<String> certSupplier) {
        this.signer = signer;
        this.kid = kid;
        this.type = type;
        this.certSupplier = certSupplier;
    }

    @Override
    public String toToken(JwtClaims claims) {
        JwsCompactProducer producer = new JwsCompactProducer(createJWSHeader(), JwtJsonUtils.serializeClaims(claims), false);
        return producer.signWith(signer);
    }

    private JwsHeaders createJWSHeader() {
        JwsHeaders headers;
        if (SignatureAlgorithm.ES256 == signer.getAlgorithm()) {
            headers = new IdpJwsHeaders(SignatureAlgorithm.ES256);
            //@AFO: A_20695-01 - Signaturalgorithmus ist BP256R1
            //@AFO: A_20521-02 - Signaturalgorithmus ist BP256R1
            //@AFO: A_20327-02 - Signaturalgorithmus ist BP256R1
            headers.setAlgorithm(CryptoConstants.SIG_ALG_BRAINPOOL_P256_R1);
        }
        else {
            headers = new JwsHeaders(signer.getAlgorithm());
            headers.setAlgorithm(signer.getAlgorithm().getJwaName());
        }
        if (this.kid != null) {
            headers.setKeyId(kid);
        }
        if (this.certSupplier != null) {
            // @AFO: A_20591-01 - Wenn ein Zertifikats-Supplier gesetzt ist, wird das Zertifikat in den 'x5c' Header des Tokens geschrieben
            // @AFO: A_20687-01 - puk_disc_sig wird im x5c-Header des Discovery Documents ausgeliefert
            headers.setX509Chain(Collections.singletonList(certSupplier.get()));
        }
        headers.setHeader(JoseConstants.HEADER_TYPE, type);
        return headers;
    }
}

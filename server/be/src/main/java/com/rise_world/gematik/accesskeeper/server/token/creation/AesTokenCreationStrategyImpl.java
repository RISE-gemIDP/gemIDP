/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.creation;

import com.rise_world.gematik.accesskeeper.common.crypt.KeyConstants;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;

import javax.crypto.SecretKey;

public class AesTokenCreationStrategyImpl implements AesTokenCreationStrategy {

    private JwsSignatureProvider signatureProvider;
    private String type;

    public AesTokenCreationStrategyImpl(JwsSignatureProvider signatureProvider, String type) {
        this.signatureProvider = signatureProvider;
        this.type = type;
    }

    @Override
    public String toToken(JwtClaims claims, SecretKey key) {
        // @AFO: GS-A_5016 symmetrische Verschlüsselung mit beigestelltem AES Schlüsselmaterial
        final EncryptAndSignStrategy strategy = new EncryptAndSignStrategy(
            JweUtils.getDirectKeyJweEncryption(key, ContentAlgorithm.A256GCM),
            signatureProvider,
            KeyConstants.PUK_IDP_SIG,
            type);
        return strategy.toToken(claims);
    }
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.crypt.kms;

import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
import com.rise_world.gematik.idp.kms.api.rest.TokenResource;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionInput;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.KeyDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;

public class KMSEcdhDirectKeyDecryptionAlgorithm implements KeyDecryptionProvider {

    private final TokenResource tokenResource;
    private final TokenType tokenType;

    public KMSEcdhDirectKeyDecryptionAlgorithm(TokenResource resource, TokenType tokenType) {
        if (!tokenType.isEcdhEsDecrypt()) {
            throw new IllegalArgumentException("tokenType %s doesn't require ecdh-es decryption".formatted(tokenType.name()));
        }

        this.tokenResource = resource;
        this.tokenType = tokenType;
    }

    @Override
    public byte[] getDecryptedContentEncryptionKey(JweDecryptionInput jweDecryptionInput) {
        JweHeaders headers = jweDecryptionInput.getJweHeaders();
        JsonWebKey publicJwk = headers.getJsonWebKey("epk");
        String apuHeader = headers.getStringProperty("apu");
        String apvHeader = headers.getStringProperty("apv");
        if (tokenType == TokenType.SEKTORAL_ID_TOKEN) {
            return tokenResource.deriveSekCEK(publicJwk.getStringProperty("x"), publicJwk.getStringProperty("y"), apuHeader, apvHeader);
        }
        else {
            return tokenResource.deriveCEK(publicJwk.getStringProperty("x"), publicJwk.getStringProperty("y"), apuHeader, apvHeader);
        }
    }

    @Override
    public KeyAlgorithm getAlgorithm() {
        return KeyAlgorithm.ECDH_ES_DIRECT;
    }

}

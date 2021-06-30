/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.crypt.kms;

import com.rise_world.gematik.idp.kms.api.rest.TokenResource;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionInput;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.KeyDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;

public class KMSEcdhDirectKeyDecryptionAlgorithm implements KeyDecryptionProvider {

    private TokenResource tokenResource;

    public KMSEcdhDirectKeyDecryptionAlgorithm(TokenResource resource) {
        this.tokenResource = resource;
    }

    @Override
    public byte[] getDecryptedContentEncryptionKey(JweDecryptionInput jweDecryptionInput) {
        JweHeaders headers = jweDecryptionInput.getJweHeaders();
        JsonWebKey publicJwk = headers.getJsonWebKey("epk");
        String apuHeader = headers.getStringProperty("apu");
        String apvHeader = headers.getStringProperty("apv");
        return tokenResource.deriveCEK(publicJwk.getStringProperty("x"), publicJwk.getStringProperty("y"), apuHeader, apvHeader);
    }

    @Override
    public KeyAlgorithm getAlgorithm() {
        return KeyAlgorithm.ECDH_ES_DIRECT;
    }

}

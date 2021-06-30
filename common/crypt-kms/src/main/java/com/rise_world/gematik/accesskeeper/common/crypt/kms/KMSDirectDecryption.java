/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.crypt.kms;

import com.rise_world.gematik.idp.kms.api.rest.TokenResource;
import com.rise_world.gematik.idp.kms.api.rest.model.TokenType;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweCompactConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionInput;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionOutput;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;

import java.util.Base64;

public class KMSDirectDecryption implements JweDecryptionProvider {
    private TokenResource tokenResource;
    private TokenType tokenType;

    public KMSDirectDecryption(TokenResource tokenResource, TokenType tokenType) {
        this.tokenResource = tokenResource;
        this.tokenType = tokenType;
    }

    @Override
    public JweDecryptionOutput decrypt(String content) {
        JweCompactConsumer consumer = new JweCompactConsumer(content);
        return doDecrypt(consumer.getJweDecryptionInput());
    }

    @Override
    public byte[] decrypt(JweDecryptionInput jweInput) {
        return doDecrypt(jweInput).getContent();
    }

    private JweDecryptionOutput doDecrypt(JweDecryptionInput jweDecryptionInput) {
        String cipherAndTag = encode(ArrayUtils.addAll(jweDecryptionInput.getEncryptedContent(), jweDecryptionInput.getAuthTag()));
        String iv = encode(jweDecryptionInput.getInitVector());
        String kid = jweDecryptionInput.getJweHeaders().getKeyId();
        String aad = getAdditionalAuthenticationDataFromHeader(jweDecryptionInput.getJweHeaders());
        byte[] decryptedContent = this.tokenResource.decryptInternalToken(cipherAndTag, iv, aad, kid, tokenType);
        return new JweDecryptionOutput(jweDecryptionInput.getJweHeaders(), decryptedContent);
    }

    private String getAdditionalAuthenticationDataFromHeader(JweHeaders header) {
        return encode(JweUtils.getAdditionalAuthenticationData(new JsonMapObjectReaderWriter().toJson(header), null));
    }

    private String encode(byte[] byteArray) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(byteArray);
    }

    @Override
    public KeyAlgorithm getKeyAlgorithm() {
        return KeyAlgorithm.DIRECT;
    }

    @Override
    public ContentAlgorithm getContentAlgorithm() {
        return ContentAlgorithm.A256GCM;
    }
}

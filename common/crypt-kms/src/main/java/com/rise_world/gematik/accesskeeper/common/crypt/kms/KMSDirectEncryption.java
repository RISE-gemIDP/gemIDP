/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.crypt.kms;

import com.rise_world.gematik.idp.kms.api.rest.TokenResource;
import com.rise_world.gematik.idp.kms.api.rest.model.EncryptedTokenData;
import com.rise_world.gematik.idp.kms.api.rest.model.TokenType;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweCompactBuilder;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionInput;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionOutput;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;

import java.util.Arrays;
import java.util.Base64;

public class KMSDirectEncryption implements JweEncryptionProvider {

    private static final int DEFAULT_AUTH_TAG_LENGTH = 128;
    private TokenResource tokenResource;
    private TokenType tokenType;

    protected KMSDirectEncryption(TokenResource tokenResource, TokenType tokenType) {
        this.tokenResource = tokenResource;
        this.tokenType = tokenType;
    }

    @Override
    public String encrypt(byte[] content, JweHeaders jweHeaders) {
        jweHeaders.setKeyEncryptionAlgorithm(KeyAlgorithm.DIRECT);
        //reserve placeholder for KMS to set key identifier for AAD
        jweHeaders.setKeyId("$KID$");

        String jsonHeader = new JsonMapObjectReaderWriter().toJson(jweHeaders);
        EncryptedTokenData encrypted = this.tokenResource.encryptInternalToken(encode(content), jsonHeader, tokenType);

        byte[] encryptedContent = encrypted.getCiphertextAndTag();
        byte[] cipher = getActualCipher(encryptedContent);
        byte[] authTag = getAuthenticationTag(encryptedContent);
        jweHeaders.setKeyId(encrypted.getKid());
        JweCompactBuilder producer = new JweCompactBuilder(jweHeaders, new byte[] {}, encrypted.getIv(), cipher, authTag);
        return producer.getJweContent();
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

    @Override
    public JweEncryptionOutput getEncryptionOutput(JweEncryptionInput jweInput) {
        throw new UnsupportedOperationException();
    }

    private byte[] getActualCipher(byte[] cipher) {
        return Arrays.copyOf(cipher, cipher.length - DEFAULT_AUTH_TAG_LENGTH / 8);
    }

    private byte[] getAuthenticationTag(byte[] cipher) {
        return Arrays.copyOfRange(cipher, cipher.length - DEFAULT_AUTH_TAG_LENGTH / 8, cipher.length);
    }
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.extraction;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.crypto.SecretKey;

import java.util.Arrays;

public class KeyVerifier {

    @JsonProperty(value = "token_key")
    private byte[] tokenKey;
    @JsonProperty(value = "code_verifier")
    private String codeVerifier;

    public String getCodeVerifier() {
        return codeVerifier;
    }

    @SuppressWarnings("unused") // required for json de-serialization
    protected void setCodeVerifier(String codeVerifier) {
        this.codeVerifier = codeVerifier;
    }

    public byte[] getTokenKey() {
        return tokenKey;
    }

    /**
     * Sets the tokenKey and if not empty initializes the needed secret key
     * @param tokenKey  byte array of aes secret key
     */
    @SuppressWarnings("unused") // required for json de-serialization
    protected void setTokenKey(byte[] tokenKey) {
        this.tokenKey = tokenKey;
    }

    public SecretKey getSecretTokenKey() {
        return new SecretKey() {

            @Override
            public String getAlgorithm() {
                return "AES";
            }

            @Override
            public String getFormat() {
                return "RAW";
            }

            @Override
            public byte[] getEncoded() {
                return tokenKey;
            }

        };
    }

    /**
     * Destroys relevant information needed for an AES secret key
     */
    public void destroy() {
        Arrays.fill(this.tokenKey, (byte) 0);
        this.tokenKey = null;
    }
}

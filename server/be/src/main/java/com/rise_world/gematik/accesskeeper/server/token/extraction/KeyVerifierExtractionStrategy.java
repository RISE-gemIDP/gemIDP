/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.extraction;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.rise_world.gematik.accesskeeper.common.crypt.DecryptionProviderFactory;
import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.common.token.extraction.ExtractionStrategy;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.EpkValidation;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.rs.security.jose.jwe.JweCompactConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweException;
import org.apache.cxf.rs.security.jose.jwe.JweException.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Component
@Qualifier("keyVerifier")
public class KeyVerifierExtractionStrategy implements ExtractionStrategy<KeyVerifier> {

    private static final Logger LOG = LoggerFactory.getLogger(KeyVerifierExtractionStrategy.class);

    private DecryptionProviderFactory decryptionFactory;
    private EpkValidation headerValidation = new EpkValidation(ErrorCodes.TOKEN_INVALID_KEY_VERIFIER);
    private ObjectMapper mapper;

    @Autowired
    public KeyVerifierExtractionStrategy(DecryptionProviderFactory decryptionFactory) {
        this.decryptionFactory = decryptionFactory;
        this.mapper = new ObjectMapper()
            .setBase64Variant(Base64Variants.MODIFIED_FOR_URL);
    }

    @Override
    public KeyVerifier extractAndValidate(String token, Map<String, Object> context) {
        KeyVerifier decryptedToken = null;
        try {
            JweCompactConsumer consumer = new JweCompactConsumer(token);
            decryptedToken = internalDecrypt(consumer);

            headerValidation.validate(consumer.getJweHeaders());
            validate(decryptedToken);
            return decryptedToken;
        }
        catch (JweException e) {
            if (decryptedToken != null) {
                decryptedToken.destroy();
            }
            LOG.warn("Failed to parse token");
            throw new AccessKeeperException(ErrorCodes.TOKEN_INVALID_KEY_VERIFIER, e);
        }
    }

    private KeyVerifier internalDecrypt(JweCompactConsumer consumer) {
        byte[] decryptedContent = ArrayUtils.EMPTY_BYTE_ARRAY;
        // @AFO: A_21319 entschlüsseln des Key Verifier und extrahieren des code_verifier
        // @AFO: A_21320 entschlüsseln des Key Verifier und extrahieren des token_key
        try {
            decryptedContent = consumer.getDecryptedContent(decryptionFactory.createDecryptionProvider(TokenType.KEY_VERIFIER));
            return this.mapper.readValue(decryptedContent, KeyVerifier.class);
        }
        catch (InvalidFormatException e) {
            if (StringUtils.contains(e.getPathReference(), ClaimUtils.TOKEN_KEY)) {
                throw new AccessKeeperException(ErrorCodes.TOKEN_INVALID_TOKEN_KEY);
            }
            if (StringUtils.contains(e.getPathReference(), ClaimUtils.CODE_VERIFIER)) {
                throw new AccessKeeperException(ErrorCodes.TOKEN_INVALID_CODE_VERIFIER);
            }
            throw new JweException(Error.CONTENT_DECRYPTION_FAILURE, e);
        }
        catch (Exception e) {
            throw new JweException(Error.CONTENT_DECRYPTION_FAILURE, e);
        }
        finally {
            // erase content
            Arrays.fill(decryptedContent, (byte) 0);
            decryptedContent = null;
        }
    }

    private void validate(KeyVerifier claims) {
        String codeVerifier = claims.getCodeVerifier();
        byte[] tokenKey = claims.getTokenKey();

        if (codeVerifier == null || codeVerifier.trim().isEmpty()) {
            LOG.warn("code_verifier is empty");
            throw new AccessKeeperException(ErrorCodes.TOKEN_MISSING_CODE_VERIFIER);
        }
        if (!ClaimUtils.isValidCodeVerifier(codeVerifier)) {
            LOG.warn("code_verifier does not match unreserved URI character pattern");
            throw new AccessKeeperException(ErrorCodes.TOKEN_INVALID_CODE_VERIFIER);
        }

        if (ArrayUtils.isEmpty(tokenKey)) {
            LOG.warn("token_key is empty");
            throw new AccessKeeperException(ErrorCodes.TOKEN_MISSING_TOKEN_KEY);
        }

        if (tokenKey.length != 32) {
            LOG.warn("token_key is not proper formatted");
            throw new AccessKeeperException(ErrorCodes.TOKEN_INVALID_TOKEN_KEY);
        }
    }

}

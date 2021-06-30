/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.token.extraction.parser;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.ClaimValidation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.rs.security.jose.jwe.JweCompactConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweException;
import org.apache.cxf.rs.security.jose.jwe.JweException.Error;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsException;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Function;

/**
 * Decrypts the supplied token. A nested token is expected. The token is unboxed and exp claims are
 * checked against JWE header exp.
 */
public class JweTokenParser implements TokenParser {

    private static final Logger LOG = LoggerFactory.getLogger(JweTokenParser.class);

    private JweDecryptionProvider decryption;
    private Function<IdpJwsJwtCompactConsumer, Object> expiryExtractor;
    private ClaimValidation<JweHeaders>[] headerValidation;
    private ErrorMessage parsingError;

    @SafeVarargs
    public JweTokenParser(JweDecryptionProvider decryption, ErrorMessage parsingError, ClaimValidation<JweHeaders>... headerValidation) {
        this(decryption, parsingError, JweTokenParser::getExpiryClaim, headerValidation);
    }

    @SafeVarargs
    public JweTokenParser(JweDecryptionProvider decryption, ErrorMessage parsingError,
                          Function<IdpJwsJwtCompactConsumer, Object> expiryExtractor, ClaimValidation<JweHeaders>... headerValidation) {
        this.decryption = Validate.notNull(decryption);
        this.expiryExtractor = expiryExtractor;
        this.headerValidation = headerValidation;
        this.parsingError = parsingError;
    }

    @Override
    public IdpJwsJwtCompactConsumer parse(String token) {
        if (StringUtils.isEmpty(token)) {
            throw new AccessKeeperException(parsingError);
        }

        try {
            return parseInternal(token);
        }
        catch (JwsException | JweException e) {
            LOG.warn("Failed to parse token");
            throw new AccessKeeperException(parsingError, e);
        }
    }

    private IdpJwsJwtCompactConsumer parseInternal(String token) {
        JweCompactConsumer consumer = new JweCompactConsumer(token);

        JweHeaders header = consumer.getJweHeaders();

        for (ClaimValidation<JweHeaders> validation : headerValidation) {
            validation.validate(header);
        }

        if (!ClaimUtils.NESTED_TOKEN_CTY_VALUE.equals(header.getContentType())) {
            LOG.warn("wrong content type in encrypted token");
            throw new AccessKeeperException(parsingError);
        }

        IdpJwsJwtCompactConsumer content;
        try {
            String decryptedContent = consumer.getDecryptedContentText(decryption);
            content = ClaimUtils.unboxNestedTo(decryptedContent, IdpJwsJwtCompactConsumer::new);
        }
        catch (Exception e) {
            throw new JweException(Error.CONTENT_DECRYPTION_FAILURE, e);
        }

        if (content == null || !Objects.equals(header.getHeader(JwtConstants.CLAIM_EXPIRY), this.expiryExtractor.apply(content))) {
            throw new JweException(Error.CONTENT_DECRYPTION_FAILURE);
        }
        return content;
    }

    /**
     * Extract the expiry header of a (nested) token
     * @param content the consumer containing the payload
     * @return {@code null} if no expiry could be extracted
     */
    protected static Object getExpiryClaim(IdpJwsJwtCompactConsumer content) {
        if (content == null) {
            return null;
        }

        if (ClaimUtils.NESTED_TOKEN_CTY_VALUE.equals(content.getJwsHeaders().getContentType())) {
            return getExpiryClaim(ClaimUtils.unboxNestedTo(content.getDecodedJwsPayload(), IdpJwsJwtCompactConsumer::new));
        }

        return content.getJwtClaims().getClaim(JwtConstants.CLAIM_EXPIRY);
    }

}

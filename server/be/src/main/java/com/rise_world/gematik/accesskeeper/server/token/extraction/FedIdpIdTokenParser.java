/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.extraction;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.TokenParser;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.ClaimValidation;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.EpkValidation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.rs.security.jose.jwe.JweCompactConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweException;
import org.apache.cxf.rs.security.jose.jwe.JweException.Error;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jws.JwsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decrypts the supplied token. A JWT (not nested) token is expected.
 */
public class FedIdpIdTokenParser implements TokenParser {

    private static final Logger LOG = LoggerFactory.getLogger(FedIdpIdTokenParser.class);

    private final JweDecryptionProvider decryption;
    private final ClaimValidation<JweHeaders>[] headerValidation;

    public FedIdpIdTokenParser(JweDecryptionProvider decryption) {
        this.decryption = Validate.notNull(decryption);
        this.headerValidation = new ClaimValidation[] {new EpkValidation(JsonWebKey.EC_CURVE_P256, ErrorCodes.FEDAUTH_INVALID_ID_TOKEN)};
    }

    @Override
    public IdpJwsJwtCompactConsumer parse(String token) {
        if (StringUtils.isEmpty(token)) {
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_ID_TOKEN);
        }

        try {
            return parseInternal(token);
        }
        catch (JwsException | JweException e) {
            LOG.warn("Failed to parse token");
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_ID_TOKEN, e);
        }
    }

    private IdpJwsJwtCompactConsumer parseInternal(String token) {
        JweCompactConsumer consumer = new JweCompactConsumer(token);

        JweHeaders header = consumer.getJweHeaders();

        for (ClaimValidation<JweHeaders> validation : headerValidation) {
            validation.validate(header);
        }

        if (!ClaimUtils.JWT_CTY_VALUE.equals(header.getContentType())) {
            LOG.warn("wrong content type in encrypted token");
            throw new AccessKeeperException(ErrorCodes.FEDAUTH_INVALID_ID_TOKEN);
        }

        try {
            String decryptedContent = consumer.getDecryptedContentText(decryption);
            return new IdpJwsJwtCompactConsumer(decryptedContent);
        }
        catch (Exception e) {
            throw new JweException(Error.CONTENT_DECRYPTION_FAILURE, e);
        }
    }
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.token.extraction.parser;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlainTokenParser implements TokenParser {

    private static final Logger LOG = LoggerFactory.getLogger(PlainTokenParser.class);

    private ErrorMessage parsingError;

    public PlainTokenParser(ErrorMessage parsingError) {
        this.parsingError = parsingError;
    }

    @Override
    public IdpJwsJwtCompactConsumer parse(String token) {
        if (StringUtils.isEmpty(token)) {
            throw new AccessKeeperException(parsingError);
        }

        try {
            IdpJwsJwtCompactConsumer consumer = new IdpJwsJwtCompactConsumer(token);
            consumer.getJwtClaims();  // trigger token parsing
            return consumer;
        }
        catch (Exception e) {
            LOG.warn("Failed to parse token");
            throw new AccessKeeperException(parsingError, e);
        }
    }
}

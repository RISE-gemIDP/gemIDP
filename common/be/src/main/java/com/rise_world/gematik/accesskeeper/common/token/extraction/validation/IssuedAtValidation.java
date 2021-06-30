/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.token.extraction.validation;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;

/**
 * Validates if the token has a valid issued at
 */
public class IssuedAtValidation implements ClaimValidation<IdpJwsJwtCompactConsumer> {

    private static final Logger LOG = LoggerFactory.getLogger(IssuedAtValidation.class);

    private Clock clock;
    private ErrorMessage errorMessage;

    public IssuedAtValidation(Clock clock, ErrorMessage errorMessage) {
        this.clock = clock;
        this.errorMessage = errorMessage;
    }

    @Override
    public void validate(IdpJwsJwtCompactConsumer token) {
        JwtClaims claims = token.getJwtClaims();
        Long issuedAt = claims.getIssuedAt();
        Instant now = Instant.now(clock);

        if (issuedAt == null || Instant.ofEpochMilli(issuedAt * 1000L).isAfter(now)) {
            LOG.warn("iat is missing or invalid");
            throw new AccessKeeperException(errorMessage);
        }
    }
}

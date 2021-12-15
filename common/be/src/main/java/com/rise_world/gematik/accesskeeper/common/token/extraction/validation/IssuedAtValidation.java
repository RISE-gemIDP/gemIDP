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
import java.time.temporal.ChronoUnit;

/**
 * Validates if the token has a valid issued at
 */
public class IssuedAtValidation implements ClaimValidation<IdpJwsJwtCompactConsumer> {

    private static final Logger LOG = LoggerFactory.getLogger(IssuedAtValidation.class);

    private Clock clock;
    private ErrorMessage errorMessage;
    private long leewayMillis;

    public IssuedAtValidation(Clock clock, ErrorMessage errorMessage, long leewayMillis) {
        this.clock = clock;
        this.errorMessage = errorMessage;
        this.leewayMillis = leewayMillis;
    }

    @Override
    public void validate(IdpJwsJwtCompactConsumer token) {
        JwtClaims claims = token.getJwtClaims();
        Long issuedAt = claims.getIssuedAt();
        if (issuedAt == null) {
            LOG.warn("iat is missing");
            throw new AccessKeeperException(errorMessage);
        }

        // calculate issuedAt considering a configured leeway
        Instant issuedAtInstantWithLeeway = Instant.ofEpochSecond(issuedAt).minus(leewayMillis, ChronoUnit.MILLIS);

        Instant now = Instant.now(clock);
        if (issuedAtInstantWithLeeway.isAfter(now)) {

            LOG.warn("iat is invalid [issuedAtInstantWithLeeway={}], [now={}]", issuedAtInstantWithLeeway, now);
            throw new AccessKeeperException(errorMessage);
        }
    }
}

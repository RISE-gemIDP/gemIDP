/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.token.extraction.validation;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;

public abstract class ExpirationValidation<T> implements ClaimValidation<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ExpirationValidation.class);

    private Clock clock;
    private ErrorMessage missingClaim;
    private ErrorMessage expiredClaim;

    protected ExpirationValidation(Clock clock, ErrorMessage failOnMissing, ErrorMessage failOnExpiry) {
        this.missingClaim = failOnMissing;
        this.expiredClaim = failOnExpiry;
        this.clock = clock;
    }

    /**
     * Validates the 'exp' claim of the claim map against the current timestamp
     *
     * @param claims the claim map
     */
    public void validateExpiry(JsonMapObject claims) {
        Long expiryTime = ClaimUtils.getLongPropertyWithoutException(claims, JwtConstants.CLAIM_EXPIRY);
        if (expiryTime == null) {
            LOG.warn("Expiration is missing");
            throw new AccessKeeperException(missingClaim);
        }
        else {
            Instant now = Instant.now(clock);
            Instant expires = Instant.ofEpochMilli(expiryTime * 1000L);

            if (expires.isBefore(now)) {
                LOG.warn("Token is expired [exp={}]", expires.getEpochSecond());
                throw new AccessKeeperException(expiredClaim);
            }
        }
    }
}

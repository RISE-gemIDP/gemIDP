/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.token.extraction.validation;

import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;

import java.time.Clock;

public class TokenExpiry extends ExpirationValidation<IdpJwsJwtCompactConsumer> {

    public TokenExpiry(Clock clock, ErrorMessage failOnMissing, ErrorMessage failOnExpiry) {
        super(clock, failOnMissing, failOnExpiry);
    }

    @Override
    public void validate(IdpJwsJwtCompactConsumer claims) {
        validateExpiry(claims.getJwtClaims());
    }

}

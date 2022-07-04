/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.token.extraction;

import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.TokenParser;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.ClaimValidation;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class AbstractClaimExtractionStrategy implements ClaimExtractionStrategy {

    private TokenParser tokenParser;
    private List<ClaimValidation<IdpJwsJwtCompactConsumer>> validationList = new ArrayList<>();

    @SafeVarargs
    protected AbstractClaimExtractionStrategy(TokenParser tokenParser, ClaimValidation<IdpJwsJwtCompactConsumer>... validation) {
        this.tokenParser = tokenParser;
        this.validationList.addAll(Arrays.asList(validation));
    }

    @Override
    public JwtClaims extractAndValidate(String token, Map<String, Object> context) {
        IdpJwsJwtCompactConsumer tokenConsumer = tokenParser.parse(token);
        for (ClaimValidation<IdpJwsJwtCompactConsumer> tokenValidation : validationList) {
            tokenValidation.validate(tokenConsumer);
        }

        return extractInternal(tokenConsumer, context);
    }

    /**
     * Takes the consumer (parsed and validated token) as input and extracts the claims
     * @param tokenConsumer the consumer containing the token
     * @param context additional context information that may be used for extract and validate
     * @return the extracted claims
     */
    protected abstract JwtClaims extractInternal(IdpJwsJwtCompactConsumer tokenConsumer, Map<String, Object> context);
}

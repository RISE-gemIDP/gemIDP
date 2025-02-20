/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.extraction.validation;

import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;
import com.rise_world.gematik.accesskeeper.server.service.ConfigService;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.ClaimValidation;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;

/**
 * Validates token type, issuer and required claims
 */
public class ContentValidation implements ClaimValidation<IdpJwsJwtCompactConsumer>  {

    private static final Logger LOG = LoggerFactory.getLogger(ContentValidation.class);

    private ConfigService configService;
    private TokenType tokenType;
    private String[] requiredClaims;
    private ErrorMessage errorMessage;

    public ContentValidation(ConfigService configService, TokenType tokenType, String[] requiredClaims, ErrorMessage errorMessage) {
        Objects.requireNonNull(tokenType);

        this.configService = configService;
        this.tokenType = tokenType;
        this.requiredClaims = Arrays.copyOf(requiredClaims, requiredClaims.length);
        this.errorMessage = errorMessage;
    }

    @Override
    public void validate(IdpJwsJwtCompactConsumer token) {
        JwtClaims claims = token.getJwtClaims();

        if (!configService.getIssuers().contains(claims.getIssuer())) {
            LOG.warn("Configured issuer and token issuer don't match");
            throw new AccessKeeperException(errorMessage);
        }
        if (!tokenType.getId().equals(claims.getProperty(ClaimUtils.TOKEN_TYPE))) {
            LOG.warn("Token type is invalid");
            throw new AccessKeeperException(errorMessage);
        }
        if (!ClaimUtils.containsAllClaims(claims, requiredClaims)) {
            LOG.error("Token doesn't contain all relevant claims");
            throw new AccessKeeperException(errorMessage);
        }
    }
}

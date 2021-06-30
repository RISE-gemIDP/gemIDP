/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.server.dto.RedeemedTokenDTO;

/**
 * Provides methods for retrieving tokens
 */
public interface TokenService {

    /**
     * Creates a RedeemTokenDTO based on the provided authorization code. Multiple integrity and semantic
     * checks will be performed on the provided parameters. If any of these checks fail, the method will
     * return with an exception with additional information about the reason for the negative result.
     *
     * @param authCode      an authorization code
     * @param keyVerifier   a key verifier token
     * @param clientId      client identification
     * @param grantType     type of grant flow used
     * @param redirectUri   uri to be redirected to
     * @return a bean representation of a token response containing an access token, an id token and an expiry
     *         time
     * @throws AccessKeeperException if any of the defined parameter checks (integrity and/or semantic) fails
     */
    RedeemedTokenDTO redeemToken(String authCode, String keyVerifier, String clientId, String grantType, String redirectUri);
}

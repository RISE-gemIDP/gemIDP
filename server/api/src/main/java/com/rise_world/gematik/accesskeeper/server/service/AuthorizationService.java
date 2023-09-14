/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.server.dto.ChallengeDTO;
import com.rise_world.gematik.accesskeeper.server.dto.OpenidProviderDTO;
import com.rise_world.gematik.accesskeeper.server.dto.RedeemedChallengeDTO;
import com.rise_world.gematik.accesskeeper.server.dto.RedeemedSsoTokenDTO;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;

/**
 * Provides business logic for the authorization endpoint
 */
public interface AuthorizationService {

    /**
     * Validates the clientId and the redirectUri. If these values are invalid, redirects are NOT allowed.
     * <p>
     * Both values are validated against the configuration
     *
     * @param clientId    the client id
     * @param redirectUri the redirectUri
     * @throws AccessKeeperException if the values are invalid
     */
    void validateClientAndRedirectUri(String clientId, String redirectUri);

    ChallengeDTO createChallenge(String responseType, String clientId, String state, String redirectUri, String scope,
                                 String codeChallenge, String codeChallengeMethod, String nonce);

    /**
     * Starts a new external authentication
     *
     * @param kkAppId             the id of a registered {@link com.rise_world.gematik.accesskeeper.server.model.SektorApp}
     * @param responseType        the Oauth2 response type
     * @param clientId            the id of a registered {@link com.rise_world.gematik.accesskeeper.server.model.Client}
     * @param state               the state parameter
     * @param redirectUri         a valid (registered) redirect uri
     * @param scope               the requested scope
     * @param codeChallenge       the code challenge
     * @param codeChallengeMethod the code challenge method (must be 'S256')
     * @param nonce               the nonce (optional)
     * @return authorization request url for sector application
     */
    String startExternalAuthorization(String kkAppId, String responseType, String clientId, String state, String redirectUri, String scope,
                                      String codeChallenge, String codeChallengeMethod, String nonce);

    RedeemedChallengeDTO redeemExternalAuthCode(String authorizationCode, String state, String kkAppRedirectUri);

    /**
     * Parses and validates the signed challenge and creates the auth_code and the sso token
     *
     * @param signedChallenge the signed challenge
     * @return the created tokens
     */
    RedeemedChallengeDTO processSignedChallenge(String signedChallenge);

    /**
     * Parses and validates the encrypted signed authentication data and creates the auth_code and the sso token.
     * <p>
     * This method is used to perform an authentication based on a pre-registered "pairing".
     *
     * @param encryptedSignedAuthenticationData the encrypted signed authentication data
     * @return the created tokens
     */
    RedeemedChallengeDTO processEncryptedSignedAuthenticationData(String encryptedSignedAuthenticationData);


    /**
     * Parses and validates an encrypted SSO token and an unsigned challenge (not signed by the client, but signed by the IdP!) and issues an authoriziation code
     *
     * @param ssoToken the encrypted and signed SSO token
     * @param unsignedChallenge the unsigned challenge (contains the new code challenge and nonce)
     * @return the created tokens
     */
    RedeemedSsoTokenDTO redeemSsoToken(String ssoToken, String unsignedChallenge);

    /**
     * Starts a new federated authentication
     *
     * @param idpIss                 the issuer value of a registered {@link OpenidProviderDTO}
     * @param responseType           the Oauth2 response type
     * @param clientId               the id of a registered {@link com.rise_world.gematik.accesskeeper.server.model.Client}
     * @param appState               the app state parameter
     * @param redirectUri            a valid (registered) redirect uri
     * @param scope                  the requested scope
     * @param appCodeChallenge       the app code challenge
     * @param appCodeChallengeMethod the app code challenge method (must be 'S256')
     * @param appNonce               the app nonce (optional)
     * @return authorization request url for sector application
     */
    String startFederatedAuthorization(String idpIss, String responseType, String clientId, String appState, String redirectUri, String scope,
                                       String appCodeChallenge, String appCodeChallengeMethod, String appNonce);

    /**
     * Redeems the authorization code issued by the federation IDP
     *
     * @param authorizationCode the authorization code issued by the federation IDP
     * @param state             the state parameter
     * @return the redeemed tokens
     */
    RedeemedChallengeDTO redeemFedAuthCode(String authorizationCode, String state);
}

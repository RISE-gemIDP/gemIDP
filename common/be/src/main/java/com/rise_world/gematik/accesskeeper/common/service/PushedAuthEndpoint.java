/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Endpoint definition for pushed authorization requests endpoint.
 */
@Path("/")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
public interface PushedAuthEndpoint {

    @POST
    Response pushedAuthorizationRequest(@FormParam("client_id") String clientId,
                                        @FormParam("state") String state,
                                        @FormParam("redirect_uri") String redirectURI,
                                        @FormParam("code_challenge") String codeChallenge,
                                        @FormParam("code_challenge_method") String codeChallengeMethod,
                                        @FormParam("response_type") String responseType,
                                        @FormParam("nonce") String nonce,
                                        @FormParam("scope") String scope,
                                        @FormParam("acr_values") String acrValues,
                                        @FormParam("client_assertion_type") String clientAssertionType);
}

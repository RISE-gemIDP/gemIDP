/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * Token endpoint definition for external idps
 */
@Path("/")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
public interface ExtTokenEndpoint {

    @POST
    // @AFO: A_22265 - Abbildung des Tokenendpunkts des sektoralen IDPs laut OAuth2 Spezifikation
    Map<String, String> redeem(@FormParam("code") String authCode,
                               @FormParam("code_verifier") String codeVerifier,
                               @FormParam("client_id") String clientId,
                               @FormParam("client_assertion_type") String clientAssertionType,
                               @FormParam("client_assertion") String clientAssertion,
                               @FormParam("grant_type") String grantType,
                               @FormParam("redirect_uri") String redirectUri,
                               @FormParam("refresh_token") String refreshToken);
}

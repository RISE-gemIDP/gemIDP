/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.api.pairing;

import com.rise_world.gematik.idp.server.api.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Internal pairing verification endpoint
 */
@Path("/idpinternal/pairingdienst")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "PAIRING")
public interface PairingVerificationInternalEndpoint {

    /**
     * Validates an authentication request signed with PrK_SE_AUT
     *
     * @param signedAuthenticationData the signed authentication request (contains the signed challenge)
     * @return the signed challenge
     */
    @POST
    @Path("/authenticateClientDevice")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Erfolgreiche Prüfung der SignedAuthenticationData",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthenticationResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Fehlerfall, wenn Umleitung nicht möglich",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))

        ),
        @ApiResponse(
            responseCode = "500",
            description = "Serverfehler",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    AuthenticationResponse authenticateClientDevice(
        @RequestBody(required = true)
            SignedAuthenticationDataRequest signedAuthenticationData
    );
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.sslmate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Endpoint for SslMate CT Search API
 */
@Path("/issuances")
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)
@Produces(MediaType.APPLICATION_JSON)
public interface SslMateMonitor {

    /**
     * Use this endpoint to list all certificate issuances for a domain.
     *
     * @param domain domain to be checked
     * @param includeSub If true, also return issuances that are valid for sub-domains (of any depth) of domain. Default: false.
     * @param matchWildcards If true, also return issuances for wildcard DNS names that match domain.
     * @param after Return issuances that were discovered by SSLMate after the issuance with the specified ID.
     * @param expand Include the given field in the response
     * @return issuances that are valid for the given DNS name
     */
    @GET
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    List<CertificateTransparencyRecord> issuances(@QueryParam("domain") String domain,
                                                  @QueryParam("include_subdomains") boolean includeSub,
                                                  @QueryParam("match_wildcards") boolean matchWildcards,
                                                  @QueryParam("after") String after,
                                                  @QueryParam("expand") List<String> expand);
}

/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.service;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Custom FederationConfigurationEndpoint that supports multi-tenancy.
 */
@Path("/")
public interface MultiTenantFederationConfigurationEndpoint {

    @GET
    @Path("/.well-known/openid-federation")
    @Produces({"application/entity-statement+jwt", APPLICATION_JSON})
    String getFederationEntity();

    @GET
    @Path("/.well-known/openid-federation/{tenant-identifier}")
    @Produces({"application/entity-statement+jwt", APPLICATION_JSON})
    String getFederationEntity(@PathParam("tenant-identifier") String tenantIdentifier);

    @GET
    @Path("/{tenant-identifier}/.well-known/openid-federation")
    @Produces({"application/entity-statement+jwt", APPLICATION_JSON})
    String getFederationEntityFromAlternativePath(@PathParam("tenant-identifier") String tenantIdentifier);
}

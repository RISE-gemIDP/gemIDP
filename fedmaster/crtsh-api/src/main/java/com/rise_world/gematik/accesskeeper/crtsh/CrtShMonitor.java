/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.crtsh;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * Endpoints for crt.sh API
 */
@Path("/")
public interface CrtShMonitor {

    String EXCLUDE_VALUE = "expired";
    String MATCH_VALUE = "=";
    String DEDUPLICATION_VALUE = "Y";

    String OPTION_OCSP = "ocsp";

    /**
     * Searches certificate records for the given {@code domain}
     *
     * @param domain        domain to be checked
     * @param exclude       filters the result {@link #EXCLUDE_VALUE}
     * @param match         specifies the match operation {@link #MATCH_VALUE}
     * @param deduplication filter pre certificates {@link #DEDUPLICATION_VALUE}
     * @return list of {@link CrtShRecord CrtShRecords}
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<CrtShRecord> fetch(@QueryParam("Identity") String domain,
                            @QueryParam("exclude") String exclude,
                            @QueryParam("match") String match,
                            @QueryParam("deduplication") String deduplication);

    /**
     * Fetches the certificate for the given {@link CrtShRecord} id
     *
     * @param certificateId crt.sh id of the {@link CrtShRecord}
     * @return the DER certificate
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String certificate(@QueryParam("d") String certificateId);

    /**
     * Loads the html for the given {@code certificateId} <strong>with</strong>
     * ocsp revocation information.
     *
     * @param certificateId the id of the certificate
     * @param option        {@link #OPTION_OCSP}
     * @return html for the given {@code certificateId} as string
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    String certificatePage(@QueryParam("id") String certificateId,
                           @QueryParam("opt") String option);

}

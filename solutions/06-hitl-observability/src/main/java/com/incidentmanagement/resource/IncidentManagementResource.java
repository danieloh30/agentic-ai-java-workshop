package com.incidentmanagement.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

import com.incidentmanagement.service.IncidentManagementService;

/**
 * REST resource for incident management operations.
 * Uses blocking processing for AI agent workflows.
 */
@Path("/incident-management")
public class IncidentManagementResource {

    @Inject
    IncidentManagementService incidentManagementService;

    /**
     * Process an incident report.
     *
     * @param incidentNumber The incident number
     * @param feedback The incident report details
     * @return Uni that completes with the result
     */
    @POST
    @Path("/process/{incidentNumber}")
    @Blocking
    public Uni<Response> processIncident(Integer incidentNumber, @RestQuery String feedback) {

        return incidentManagementService.processIncident(incidentNumber, feedback != null ? feedback : "")
            .onItem().transform(result -> Response.ok(result).build())
            .onFailure().recoverWithItem(e -> {
                Log.error(e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error processing incident: " + e.getMessage())
                        .build();
            });
    }

    @GET
    @Path("/report")
    @Produces(MediaType.TEXT_HTML)
    public Response report() {
        return Response.ok(incidentManagementService.report()).build();
    }
}

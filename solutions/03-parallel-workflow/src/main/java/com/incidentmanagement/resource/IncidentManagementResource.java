package com.incidentmanagement.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.logging.Log;

import com.incidentmanagement.service.IncidentManagementService;

/**
 * REST resource for incident management operations.
 */
@Path("/incident-management")
public class IncidentManagementResource {

    @Inject
    IncidentManagementService incidentManagementService;

    /**
     * Process an incident.
     *
     * @param incidentNumber The incident number
     * @param report Optional report details
     * @return Result of the processing
     */
    @POST
    @Path("/process/{incidentNumber}")
    public Response processIncident(Integer incidentNumber, @RestQuery String report) {

        try {
            String result = incidentManagementService.processIncident(incidentNumber, report != null ? report : "");
            return Response.ok(result).build();
        } catch (Exception e) {
            Log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error processing incident: " + e.getMessage())
                    .build();
        }
    }
}

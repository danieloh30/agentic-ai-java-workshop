package com.incidentmanagement.resource;

import com.incidentmanagement.model.IncidentInfo;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * REST resource for incident operations.
 */
@Path("/incidents")
public class IncidentResource {

    /**
     * Get all incidents in the system.
     *
     * @return List of all incidents
     */
    @GET
    public List<IncidentInfo> getAllIncidents() {
        return IncidentInfo.listAll();
    }

    /**
     * Get a specific incident by its ID.
     *
     * @param id The incident ID
     * @return The incident with the specified ID, or 404 if not found
     */
    @GET
    @Path("/{id}")
    public Response getIncidentById(@PathParam("id") Integer id) {
        IncidentInfo incident = IncidentInfo.findById(id);
        if (incident == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Incident with ID " + id + " not found")
                    .build();
        }
        return Response.ok(incident).build();
    }
}

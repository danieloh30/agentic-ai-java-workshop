package com.incidentmanagement.resource;

import com.incidentmanagement.model.IncidentInfo;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/incidents")
public class IncidentResource {

    @GET
    public List<IncidentInfo> getAllIncidents() {
        return IncidentInfo.listAll();
    }

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

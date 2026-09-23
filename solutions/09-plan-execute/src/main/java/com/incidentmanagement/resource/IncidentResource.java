package com.incidentmanagement.resource;

import com.incidentmanagement.model.IncidentInfo;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import java.util.List;

@Path("/incidents")
public class IncidentResource {

    @GET
    public List<IncidentInfo> getAllIncidents() {
        return IncidentInfo.listAll();
    }

    @GET
    @Path("/{id}")
    public IncidentInfo getIncidentById(Integer id) {
        IncidentInfo incident = IncidentInfo.findById(id);
        if (incident == null) {
            throw new NotFoundException("Incident with ID " + id + " not found");
        }
        return incident;
    }
}

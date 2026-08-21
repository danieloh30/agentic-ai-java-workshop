package com.incidentmanagement.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestQuery;

import com.incidentmanagement.service.IncidentManagementService;

@Path("/incident-management")
public class IncidentManagementResource {

    @Inject
    IncidentManagementService incidentManagementService;

    @POST
    @Path("/process/{incidentNumber}")
    public String processIncident(Integer incidentNumber, @RestQuery @DefaultValue("") String report) {
        return incidentManagementService.processIncident(incidentNumber, report);
    }
}

package com.incidentmanagement.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestQuery;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

import com.incidentmanagement.service.IncidentManagementService;

@Path("/incident-management")
public class IncidentManagementResource {

    @Inject
    IncidentManagementService incidentManagementService;

    @POST
    @Path("/process/{incidentNumber}")
    @Blocking
    public Uni<String> processIncident(Integer incidentNumber, @RestQuery @DefaultValue("") String feedback) {
        return incidentManagementService.processIncident(incidentNumber, feedback);
    }

    @GET
    @Path("/report")
    @Produces(MediaType.TEXT_HTML)
    public String report() {
        return incidentManagementService.report();
    }
}

package com.incidentmanagement.resource;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.incidentmanagement.agentic.workflow.IncidentReportFlow;
import com.incidentmanagement.model.IncidentInfo;

import io.quarkus.logging.Log;

@Path("/incident-report")
@Produces(MediaType.APPLICATION_JSON)
public class ReportResource {

    @Inject
    IncidentReportFlow reportFlow;

    @POST
    @Path("/{incidentId}")
    public Response generateReport(@PathParam("incidentId") Integer incidentId) {
        IncidentInfo incident = IncidentInfo.findById(incidentId);
        if (incident == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Incident not found: " + incidentId))
                    .build();
        }

        Log.infof("Starting report quality loop for incident #%d (%s/%s %s)",
                incidentId, incident.system, incident.service, incident.priority);

        try {
            Map<String, Object> result = reportFlow.generateReport(incident);
            Log.infof("Report quality loop completed for incident #%d — final score: %s",
                    incidentId, result.get("score"));
            return Response.ok(result).build();
        } catch (Exception e) {
            Log.errorf(e, "Report generation failed for incident #%d", incidentId);
            return Response.serverError()
                    .entity(Map.of("error", "Report generation failed: " + e.getMessage()))
                    .build();
        }
    }
}

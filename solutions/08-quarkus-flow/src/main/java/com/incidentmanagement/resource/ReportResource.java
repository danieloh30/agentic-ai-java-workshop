package com.incidentmanagement.resource;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.incidentmanagement.agentic.workflow.IncidentReportFlow;
import com.incidentmanagement.model.IncidentInfo;

import io.quarkus.logging.Log;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

@Path("/incident-report")
@Produces(MediaType.APPLICATION_JSON)
public class ReportResource {

    @Inject
    IncidentReportFlow reportFlow;

    @POST
    @Path("/{incidentId}")
    public Map<String, Object> generateReport(Integer incidentId) {
        IncidentInfo incident = IncidentInfo.findById(incidentId);
        if (incident == null) {
            throw new NotFoundException("Incident not found: " + incidentId);
        }

        Log.infof("Starting report quality loop for incident #%d (%s/%s %s)",
                incidentId, incident.system, incident.service, incident.priority);

        Map<String, Object> result = reportFlow.generateReport(incident);
        Log.infof("Report quality loop completed for incident #%d — final score: %s",
                incidentId, result.get("score"));
        return result;
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, String>> mapGeneral(Exception e) {
        Log.error("Report generation failed", e);
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR,
                Map.of("error", "Report generation failed: " + e.getMessage()));
    }
}

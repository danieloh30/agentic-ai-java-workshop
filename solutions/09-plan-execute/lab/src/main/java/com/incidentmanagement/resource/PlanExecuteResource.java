package com.incidentmanagement.resource;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import com.incidentmanagement.agentic.agents.IncidentPlannerAgent;
import com.incidentmanagement.model.IncidentInfo;

import io.quarkus.logging.Log;

/**
 * Runs the declarative planner for an incident and returns its consolidated result.
 * Watch the dev-mode log (or the Agentic Dev UI topology) to see which specialists
 * the planner chose to call and in what order — that is the executed plan.
 */
@Path("/incident-plan")
@Produces(MediaType.APPLICATION_JSON)
public class PlanExecuteResource {

    @Inject
    IncidentPlannerAgent incidentPlannerAgent;

    @POST
    @Path("/{incidentId}")
    @Transactional
    public Map<String, Object> plan(Integer incidentId, @RestQuery @DefaultValue("") String report) {
        IncidentInfo incident = IncidentInfo.findById(incidentId);
        if (incident == null) {
            throw new NotFoundException("Incident not found: " + incidentId);
        }
        Log.infof("Starting Plan & Execute for incident #%d (%s/%s %s)",
                incidentId, incident.system, incident.service, incident.priority);
        String result = incidentPlannerAgent.resolveIncident(incident, incidentId, report);
        return Map.of("incidentId", incidentId, "planResult", result);
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, String>> mapGeneral(Exception e) {
        Log.error("Plan & Execute failed", e);
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR,
                Map.of("error", "Plan & Execute failed: " + e.getMessage()));
    }
}

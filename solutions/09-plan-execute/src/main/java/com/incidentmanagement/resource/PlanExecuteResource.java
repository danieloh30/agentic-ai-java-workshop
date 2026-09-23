package com.incidentmanagement.resource;

import java.util.LinkedHashMap;
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

import com.incidentmanagement.agentic.workflow.PlanExecuteFlow;
import com.incidentmanagement.model.ExecutionPlan;
import com.incidentmanagement.model.IncidentInfo;

import io.quarkus.logging.Log;

/**
 * Runs Plan &amp; Execute for one incident and returns the plan the LLM chose alongside
 * every specialist's output — so you can see the whole thing from a single curl call,
 * no Dev UI required.
 */
@Path("/incident-plan")
@Produces(MediaType.APPLICATION_JSON)
public class PlanExecuteResource {

    @Inject
    PlanExecuteFlow planExecuteFlow;

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

        Map<String, Object> state = planExecuteFlow.resolve(incident, incidentId, report);

        // Shape the shared scope state into a tidy, curl-friendly response.
        var response = new LinkedHashMap<String, Object>();
        response.put("incidentId", incidentId);
        if (state.get("plan") instanceof ExecutionPlan p) {
            response.put("plan", p.steps());
            response.put("planRationale", p.rationale());
        }
        response.put("iterations", state.getOrDefault("iteration", 0));
        response.put("resolved", state.getOrDefault("resolved", false));
        response.put("diagnosis", state.getOrDefault("diagnosis", ""));
        response.put("mitigation", state.getOrDefault("mitigation", ""));
        response.put("verification", state.getOrDefault("verification", ""));
        response.put("communication", state.getOrDefault("communication", ""));
        return response;
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, String>> mapGeneral(Exception e) {
        Log.error("Plan & Execute failed", e);
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR,
                Map.of("error", "Plan & Execute failed: " + e.getMessage()));
    }
}

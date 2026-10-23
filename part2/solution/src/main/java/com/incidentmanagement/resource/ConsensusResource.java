package com.incidentmanagement.resource;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import com.incidentmanagement.agentic.workflow.ConsensusWorkflow;
import com.incidentmanagement.model.ConsensusResult;
import com.incidentmanagement.model.IncidentInfo;

import io.quarkus.logging.Log;

/**
 * Runs the Consensus &amp; Voting workflow for one incident: three persona voters assess it
 * in parallel and their ballots are tallied into a single {@link ConsensusResult}. The full
 * result — the winning action <em>and</em> every individual vote — comes back in one call.
 *
 * <p>The optional operator report is the request body (text/plain), e.g.
 * {@code curl -X POST .../incident-consensus/2 -H 'Content-Type: text/plain' --data 'pods OOMKilled'}.
 */
@Path("/incident-consensus")
@Produces(MediaType.APPLICATION_JSON)
public class ConsensusResource {

    @Inject
    ConsensusWorkflow consensusWorkflow;

    @POST
    @Path("/{incidentId}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional
    public ConsensusResult decide(Integer incidentId, String report) {
        IncidentInfo incident = IncidentInfo.findById(incidentId);
        if (incident == null) {
            throw new NotFoundException("Incident not found: " + incidentId);
        }
        Log.infof("Starting Consensus & Voting for incident #%d (%s/%s P%s)",
                incidentId, incident.system, incident.service, incident.priority);

        return consensusWorkflow.decide(incident, report != null ? report : "");
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, String>> mapGeneral(Exception e) {
        Log.error("Consensus & Voting failed", e);
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR,
                Map.of("error", "Consensus & Voting failed: " + e.getMessage()));
    }
}

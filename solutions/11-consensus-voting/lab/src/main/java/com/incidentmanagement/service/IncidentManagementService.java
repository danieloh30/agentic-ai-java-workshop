package com.incidentmanagement.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.incidentmanagement.agentic.workflow.ConsensusWorkflow;
import com.incidentmanagement.model.ConsensusResult;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.IncidentStatus;
import com.incidentmanagement.model.Vote;

import io.quarkus.logging.Log;

/**
 * Runs Consensus &amp; Voting for an incident and returns a readable summary. Wired to the
 * dashboard's "Process Incident" button ({@code POST /incident-management/process/{id}}).
 *
 * <p>For the raw structured result (every ballot as JSON), call
 * {@code POST /incident-consensus/{id}} instead.
 */
@ApplicationScoped
public class IncidentManagementService {

    @Inject
    ConsensusWorkflow consensusWorkflow;

    @Transactional
    public String processIncident(Integer incidentNumber, String report) {
        IncidentInfo incident = IncidentInfo.findById(incidentNumber);
        if (incident == null) {
            return "Incident not found with number: " + incidentNumber;
        }

        ConsensusResult result = consensusWorkflow.decide(incident, report == null ? "" : report);
        Log.infof("Consensus for incident #%d: %s (%d/%d agreed, unanimous=%b)",
                incidentNumber, result.decision(), result.agreement(),
                result.totalVoters(), result.unanimous());

        StringBuilder summary = new StringBuilder();
        summary.append("Consensus decision: ").append(result.decision())
                .append(result.unanimous() ? " (unanimous)"
                        : " (" + result.agreement() + " of " + result.totalVoters() + " voters)")
                .append("\n\nBallots:");
        for (Vote v : result.votes()) {
            summary.append("\n  • ").append(v.action())
                    .append(" (confidence ").append(v.confidence()).append(") — ")
                    .append(v.rationale());
        }

        incident.status = IncidentStatus.IN_PROGRESS;
        incident.persist();

        return summary.toString();
    }
}

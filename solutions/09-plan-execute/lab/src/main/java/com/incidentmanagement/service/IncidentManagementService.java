package com.incidentmanagement.service;

import java.util.Map;
import java.util.List;
import com.incidentmanagement.evidence.DeploymentEvidence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.incidentmanagement.agentic.workflow.PlanExecuteFlow;
import com.incidentmanagement.model.ExecutionPlan;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.IncidentStatus;
import io.quarkus.logging.Log;

/**
 * Drives an incident through the {@link PlanExecuteFlow} (Plan &amp; Execute) and persists
 * a readable summary of the outcome. Wired to the dashboard's "Process Incident" button
 * ({@code POST /incident-management/process/{id}}).
 */
@ApplicationScoped
public class IncidentManagementService {

    @Inject
    PlanExecuteFlow planExecuteFlow;

    @Transactional
    public String processIncident(Integer incidentNumber, String report) {
        IncidentInfo incidentInfo = IncidentInfo.findById(incidentNumber);
        if (incidentInfo == null) {
            return "Incident not found with number: " + incidentNumber;
        }

        Map<String, Object> state = planExecuteFlow.resolve(incidentInfo, incidentNumber, report);
        Log.infof("Plan & Execute finished for incident #%d", incidentNumber);

        String summary = summarize(state);
        // These specialists recommend work; they do not execute or observe production recovery.
        incidentInfo.status = IncidentStatus.IN_PROGRESS;
        incidentInfo.description = summary;
        incidentInfo.persist();

        return summary;
    }

    private static String summarize(Map<String, Object> state) {
        var sb = new StringBuilder();
        sb.append("Outcome: ").append(state.get("outcome")).append("\n\n");
        if (state.get("evidence") instanceof DeploymentEvidence evidence) {
            sb.append("Deployment evidence (workshop fixture): ").append(evidence.status()).append("\n")
                    .append("Service: ").append(evidence.service()).append("\n")
                    .append("Release: ").append(evidence.deployment()).append("\n")
                    .append(evidence.observation()).append("\n\n");
        }
        if (state.get("history") instanceof List<?> history) {
            sb.append("Iteration history:\n");
            for (Object entry : history) {
                if (entry instanceof Map<?, ?> iteration) {
                    sb.append("Round ").append(iteration.get("iteration")).append(": ")
                            .append(Boolean.TRUE.equals(iteration.get("accepted")) ? "Plan accepted" : "Plan rejected")
                            .append("\n");
                    if (iteration.get("plan") instanceof ExecutionPlan plan)
                        sb.append(plan.steps()).append(" — ").append(plan.rationale()).append("\n");
                    sb.append("Feedback: ").append(iteration.get("feedback")).append("\n\n");
                }
            }
        }
        if (state.get("plan") instanceof ExecutionPlan p) {
            sb.append("Plan: ").append(p.steps()).append(" — ").append(p.rationale()).append("\n\n");
        }
        appendIfPresent(sb, "Diagnosis", state.get("diagnosis"));
        appendIfPresent(sb, "Mitigation", state.get("mitigation"));
        appendIfPresent(sb, "Verification", state.get("verification"));
        appendIfPresent(sb, "Communication", state.get("communication"));
        sb.append("Recommendation accepted by reviewer: ").append(state.getOrDefault("recommendationReady", false));
        return sb.toString();
    }

    private static void appendIfPresent(StringBuilder sb, String label, Object value) {
        if (value instanceof String s && !s.isBlank()) {
            sb.append(label).append(": ").append(s).append("\n\n");
        }
    }
}

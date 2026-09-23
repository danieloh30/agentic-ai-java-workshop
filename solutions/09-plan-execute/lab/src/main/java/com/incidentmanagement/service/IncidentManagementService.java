package com.incidentmanagement.service;

import java.util.Map;

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
        incidentInfo.status = Boolean.TRUE.equals(state.get("resolved"))
                ? IncidentStatus.RESOLVED : IncidentStatus.IN_PROGRESS;
        incidentInfo.description = summary;
        incidentInfo.persist();

        return summary;
    }

    private static String summarize(Map<String, Object> state) {
        var sb = new StringBuilder();
        if (state.get("plan") instanceof ExecutionPlan p) {
            sb.append("Plan: ").append(p.steps()).append(" — ").append(p.rationale()).append("\n\n");
        }
        appendIfPresent(sb, "Diagnosis", state.get("diagnosis"));
        appendIfPresent(sb, "Mitigation", state.get("mitigation"));
        appendIfPresent(sb, "Verification", state.get("verification"));
        appendIfPresent(sb, "Communication", state.get("communication"));
        sb.append("Resolved: ").append(state.getOrDefault("resolved", false));
        return sb.toString();
    }

    private static void appendIfPresent(StringBuilder sb, String label, Object value) {
        if (value instanceof String s && !s.isBlank()) {
            sb.append(label).append(": ").append(s).append("\n\n");
        }
    }
}

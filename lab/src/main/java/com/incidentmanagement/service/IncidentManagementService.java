package com.incidentmanagement.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.incidentmanagement.agentic.agents.DiagnosticAgent;
import com.incidentmanagement.agentic.workflow.IncidentAnalysisWorkflow;
import com.incidentmanagement.agentic.workflow.IncidentProcessingWorkflow;
import com.incidentmanagement.agentic.workflow.TriageWorkflow;
import com.incidentmanagement.model.IncidentOutcome;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.IncidentStatus;
import com.incidentmanagement.model.AnalysisTask;
import io.quarkus.logging.Log;

import java.util.List;

@ApplicationScoped
public class IncidentManagementService {

    @Inject
    Instance<IncidentProcessingWorkflow> incidentProcessingWorkflow;

    @Inject
    Instance<DiagnosticAgent> diagnosticAgent;

    @Inject
    Instance<IncidentAnalysisWorkflow> incidentAnalysisWorkflow;

    @Inject
    Instance<TriageWorkflow> triageWorkflow;

    @Transactional
    public String processIncident(Integer incidentNumber, String report) {
        IncidentInfo incidentInfo = IncidentInfo.findById(incidentNumber);
        if (incidentInfo == null) {
            return "Incident not found with number: " + incidentNumber;
        }

        if (incidentProcessingWorkflow.isResolvable()) {
            List<AnalysisTask> tasks = List.of(
                    AnalysisTask.severity(),
                    AnalysisTask.impact(),
                    AnalysisTask.resolution()
            );

            IncidentOutcome incidentOutcome = incidentProcessingWorkflow.get().processIncident(
                    tasks,
                    incidentInfo,
                    incidentNumber,
                    report);

            Log.info("ResolutionAgent updating...");
            Log.infof("  └─ Action: %s → %s", incidentOutcome.incidentAction(), incidentOutcome.resolution());

            incidentInfo.description = incidentOutcome.resolution();

            incidentInfo.status = switch (incidentOutcome.incidentAction()) {
                case ESCALATE -> {
                    Log.info("Incident marked for escalation - awaiting management decision");
                    yield IncidentStatus.ESCALATED;
                }
                case INVESTIGATE -> IncidentStatus.IN_PROGRESS;
                case TRIAGE -> IncidentStatus.TRIAGING;
                case MONITOR -> incidentInfo.status;
                case RESOLVE -> IncidentStatus.RESOLVED;
            };

            incidentInfo.persist();

            return incidentOutcome.resolution();
        }

        if (triageWorkflow.isResolvable()) {
            String result = triageWorkflow.get().processTriage(incidentInfo, incidentNumber, report);

            if (result.toUpperCase().contains("TRIAGE_NOT_REQUIRED")) {
                incidentInfo.status = IncidentStatus.RESOLVED;
                incidentInfo.persist();
            }

            return result;
        }

        return "No agents available yet — complete Exercise 1 first.";
    }
}

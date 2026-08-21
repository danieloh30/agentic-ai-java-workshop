package com.incidentmanagement.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.incidentmanagement.agentic.workflow.IncidentProcessingWorkflow;
import com.incidentmanagement.model.IncidentOutcome;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.IncidentStatus;
import io.quarkus.logging.Log;

@ApplicationScoped
public class IncidentManagementService {

    @Inject
    IncidentProcessingWorkflow incidentProcessingWorkflow;

    @Transactional
    public String processIncident(Integer incidentNumber, String report) {
        IncidentInfo incidentInfo = IncidentInfo.findById(incidentNumber);
        if (incidentInfo == null) {
            return "Incident not found with number: " + incidentNumber;
        }

        Log.info("ReportAnalysisWorkflow executing...");
        Log.info("  ├─ TriageFeedbackAgent analyzing...");
        Log.info("  └─ DiagnosticFeedbackAgent analyzing...");
        Log.info("IncidentAssignmentWorkflow evaluating conditions...");

        IncidentOutcome incidentOutcome = incidentProcessingWorkflow.processIncident(incidentInfo, incidentNumber, report);

        Log.info("ResolutionAgent updating...");
        Log.infof("  └─ Action: %s → %s", incidentOutcome.incidentAction(), incidentOutcome.resolution());

        incidentInfo.description = incidentOutcome.resolution();

        incidentInfo.status = switch (incidentOutcome.incidentAction()) {
            case INVESTIGATE -> IncidentStatus.IN_PROGRESS;
            case TRIAGE -> IncidentStatus.TRIAGING;
            case ESCALATE -> IncidentStatus.ESCALATED;
            case MONITOR -> incidentInfo.status;
            case RESOLVE -> IncidentStatus.RESOLVED;
        };

        incidentInfo.persist();

        return incidentOutcome.resolution();
    }
}

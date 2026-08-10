package com.incidentmanagement.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.incidentmanagement.agentic.workflow.IncidentProcessingWorkflow;
import com.incidentmanagement.model.IncidentOutcome;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.IncidentStatus;
import io.quarkus.logging.Log;

/**
 * Service for managing IT incident processing.
 */
@ApplicationScoped
public class IncidentManagementService {

    @Inject
    IncidentProcessingWorkflow incidentProcessingWorkflow;

    /**
     * Process an incident report.
     *
     * @param incidentNumber The incident number
     * @param report Optional incident report
     * @return Result of the processing
     */
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

        // Process the incident using the workflow
        IncidentOutcome incidentOutcome = incidentProcessingWorkflow.processIncident(incidentInfo, incidentNumber, report);

        Log.info("ResolutionAgent updating...");
        Log.infof("  └─ Action: %s → %s", incidentOutcome.incidentAction(), incidentOutcome.resolution());

        // Update the incident's description with the resolution
        incidentInfo.description = incidentOutcome.resolution();

        // Update the incident status based on the required action
        switch (incidentOutcome.incidentAction()) {
            case INVESTIGATE:
                incidentInfo.status = IncidentStatus.IN_PROGRESS;
                break;
            case TRIAGE:
                incidentInfo.status = IncidentStatus.TRIAGING;
                break;
            case ESCALATE:
                incidentInfo.status = IncidentStatus.ESCALATED;
                break;
            case MONITOR:
                break;
            case RESOLVE:
                incidentInfo.status = IncidentStatus.RESOLVED;
                break;
        }

        // Persist the changes to the database
        incidentInfo.persist();

        return incidentOutcome.resolution();
    }
}

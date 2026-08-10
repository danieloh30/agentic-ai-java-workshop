package com.incidentmanagement.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.incidentmanagement.agentic.workflow.IncidentProcessingWorkflow;
import com.incidentmanagement.model.IncidentOutcome;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.IncidentStatus;
import com.incidentmanagement.model.AnalysisTask;
import io.quarkus.logging.Log;

import java.util.List;

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
     * @param report Optional report details
     * @return Result of the processing
     */
    @Transactional
    public String processIncident(Integer incidentNumber, String report) {
        IncidentInfo incidentInfo = IncidentInfo.findById(incidentNumber);
        if (incidentInfo == null) {
            return "Incident not found with number: " + incidentNumber;
        }

        // Create the list of analysis tasks
        List<AnalysisTask> tasks = List.of(
                AnalysisTask.severity(),
                AnalysisTask.impact(),
                AnalysisTask.resolution()
        );

        // Process the incident using the workflow with supervisor
        IncidentOutcome incidentOutcome = incidentProcessingWorkflow.processIncident(
                tasks,
                incidentInfo,
                incidentNumber,
                report);

        Log.info("ResolutionAgent updating...");
        Log.infof("  └─ Action: %s → %s", incidentOutcome.incidentAction(), incidentOutcome.resolution());

        // Update the incident's description with the result from ResolutionAgent
        incidentInfo.description = incidentOutcome.resolution();

        // Update the incident status based on the required action
        switch (incidentOutcome.incidentAction()) {
            case ESCALATE:
                incidentInfo.status = IncidentStatus.ESCALATED;
                Log.info("Incident marked for escalation - awaiting executive review");
                break;
            case INVESTIGATE:
                incidentInfo.status = IncidentStatus.IN_PROGRESS;
                break;
            case TRIAGE:
                incidentInfo.status = IncidentStatus.TRIAGING;
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

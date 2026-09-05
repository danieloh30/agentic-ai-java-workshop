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

        List<AnalysisTask> tasks = List.of(
                AnalysisTask.severity(),
                AnalysisTask.impact(),
                AnalysisTask.resolution()
        );

        IncidentOutcome incidentOutcome = incidentProcessingWorkflow.processIncident(
                tasks,
                incidentInfo,
                incidentNumber,
                report);

        Log.info("ResolutionAgent updating...");
        Log.infof("  └─ Incident #%d action: %s", incidentNumber, incidentOutcome.incidentAction());

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
}

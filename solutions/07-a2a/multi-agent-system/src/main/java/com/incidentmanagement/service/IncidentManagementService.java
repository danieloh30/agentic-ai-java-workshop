package com.incidentmanagement.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.incidentmanagement.agentic.workflow.IncidentProcessingWorkflow;
import com.incidentmanagement.model.IncidentOutcome;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.IncidentStatus;
import com.incidentmanagement.model.AnalysisTask;
import dev.langchain4j.data.message.ImageContent;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;

import java.util.List;

import static dev.langchain4j.agentic.observability.HtmlReportGenerator.generateReport;

/**
 * Service for managing IT incidents.
 * Uses async processing to handle Human-in-the-Loop workflow pauses.
 */
@ApplicationScoped
public class IncidentManagementService {

    @Inject
    IncidentProcessingWorkflow incidentProcessingWorkflow;

    /**
     * Process an incident report.
     * This method runs asynchronously to handle workflow pauses for human approval.
     *
     * @param incidentNumber The incident number
     * @param report Optional report details
     * @param logImage Optional screenshot of logs
     * @return Uni that completes with the result of the processing
     */
    public Uni<String> processIncident(Integer incidentNumber, String report, ImageContent logImage) {

        return Uni.createFrom().item(() -> {
            IncidentInfo incidentInfo = findIncidentInfo(incidentNumber);
            if (incidentInfo == null) {
                return "Incident not found with number: " + incidentNumber;
            }

            // Create the list of analysis tasks for parallel analysis
            List<AnalysisTask> tasks = List.of(
                    AnalysisTask.severity(),
                    AnalysisTask.impact(),
                    AnalysisTask.resolution()
            );

            // Process the incident using the workflow with supervisor
            // This may PAUSE if human approval is needed
            IncidentOutcome incidentOutcome = incidentProcessingWorkflow.processIncident(
                    tasks,
                    incidentInfo,
                    incidentNumber,
                    report,
                    logImage);

            Log.info("ResolutionAgent updating...");
            Log.infof("  └─ Action: %s → %s", incidentOutcome.incidentAction(), incidentOutcome.resolution());

            // Update the incident's description with the result from ResolutionAgent
            incidentInfo.description = incidentOutcome.resolution();

            // Update the incident status based on the required action
            switch (incidentOutcome.incidentAction()) {
                case ESCALATE:
                    incidentInfo.status = IncidentStatus.ESCALATED;
                    Log.info("Incident marked for escalation - awaiting final decision");
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

            // Persist the changes to the database in a separate transaction
            updateIncidentInfo(incidentInfo);

            return incidentOutcome.resolution();
        }).runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
    }

    /**
     * Find incident info in a read-only transaction
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    IncidentInfo findIncidentInfo(Integer incidentNumber) {
        return IncidentInfo.findById(incidentNumber);
    }

    /**
     * Update incident info in a separate transaction after workflow completes.
     * Uses merge to handle detached entity from the workflow.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void updateIncidentInfo(IncidentInfo incidentInfo) {
        IncidentInfo.getEntityManager().merge(incidentInfo);
    }

    public String report() {
        return generateReport(incidentProcessingWorkflow.agentMonitor());
    }
}

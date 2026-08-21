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

@ApplicationScoped
public class IncidentManagementService {

    @Inject
    IncidentProcessingWorkflow incidentProcessingWorkflow;

    public Uni<String> processIncident(Integer incidentNumber, String report, ImageContent logImage) {

        return Uni.createFrom().item(() -> {
            IncidentInfo incidentInfo = findIncidentInfo(incidentNumber);
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
                    report,
                    logImage);

            Log.info("ResolutionAgent updating...");
            Log.infof("  └─ Action: %s → %s", incidentOutcome.incidentAction(), incidentOutcome.resolution());

            incidentInfo.description = incidentOutcome.resolution();

            incidentInfo.status = switch (incidentOutcome.incidentAction()) {
                case ESCALATE -> {
                    Log.info("Incident marked for escalation - awaiting final decision");
                    yield IncidentStatus.ESCALATED;
                }
                case INVESTIGATE -> IncidentStatus.IN_PROGRESS;
                case TRIAGE -> IncidentStatus.TRIAGING;
                case MONITOR -> incidentInfo.status;
                case RESOLVE -> IncidentStatus.RESOLVED;
            };

            updateIncidentInfo(incidentInfo);

            return incidentOutcome.resolution();
        }).runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    IncidentInfo findIncidentInfo(Integer incidentNumber) {
        return IncidentInfo.findById(incidentNumber);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void updateIncidentInfo(IncidentInfo incidentInfo) {
        IncidentInfo.getEntityManager().merge(incidentInfo);
    }

    public String report() {
        return generateReport(incidentProcessingWorkflow.agentMonitor());
    }
}

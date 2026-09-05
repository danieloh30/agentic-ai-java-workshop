package com.incidentmanagement.agentic.workflow;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.incidentmanagement.agentic.agents.ReportCriticAgent;
import com.incidentmanagement.agentic.agents.ReportDrafterAgent;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.ReportCritique;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import io.quarkus.logging.Log;

@ApplicationScoped
public class IncidentReportFlow {

    @Inject
    ReportDrafterAgent drafterAgent;

    @Inject
    ReportCriticAgent criticAgent;

    public Map<String, Object> generateReport(IncidentInfo incident) {

        // TODO Exercise 08 — Step 2a: Create the draft agent action
        // Wrap drafterAgent.draftReport() in an AgenticServices.agentAction() lambda.
        // The lambda receives an AgenticScope. Read "feedback" from scope (with a default).
        // Track the iteration count. Call drafterAgent.draftReport() and write the result to scope.
        //
        // var draftAction = AgenticServices.agentAction(scope -> {
        //     String feedback = scope.readState("feedback", "No previous feedback. Write the first draft.");
        //     int iteration = scope.readState("iteration", 0) + 1;
        //     scope.writeState("iteration", iteration);
        //     Log.infof("Report Draft — iteration %d", iteration);
        //
        //     String report = drafterAgent.draftReport(
        //             incident.system, incident.service, incident.priority,
        //             incident.description != null ? incident.description : "",
        //             incident.status.toString(), feedback);
        //     scope.writeState("report", report);
        // });

        // TODO Exercise 08 — Step 2b: Create the critique agent action
        // Read "report" from scope. Call criticAgent.critiqueReport().
        // Write "score" and "feedback" back to scope.
        //
        // var critiqueAction = AgenticServices.agentAction(scope -> {
        //     String report = scope.readState("report", "");
        //     int iteration = scope.readState("iteration", 1);
        //
        //     ReportCritique critique = criticAgent.critiqueReport(
        //             incident.system, incident.service, incident.priority,
        //             incident.description != null ? incident.description : "",
        //             incident.status.toString(), report);
        //
        //     scope.writeState("score", critique.score());
        //     scope.writeState("feedback", critique.feedback());
        //     Log.infof("Report Critique — iteration %d: score=%d, feedback=%s",
        //             iteration, critique.score(), critique.feedback());
        // });

        // TODO Exercise 08 — Step 2c: Build the loop workflow
        // Use AgenticServices.loopBuilder() to create a loop that runs draft -> critique
        // and exits when the critic's score >= 7 (or after 3 iterations max).
        //
        // UntypedAgent workflow = AgenticServices.loopBuilder()
        //         .name("incident-report-quality-loop")
        //         .maxIterations(3)
        //         .exitCondition((scope, iteration) -> {
        //             int score = scope.readState("score", 0);
        //             return score >= 7;
        //         })
        //         .subAgents(draftAction, critiqueAction)
        //         .build();
        //
        // var result = workflow.invokeWithAgenticScope(Map.of());
        // return result.agenticScope().state();

        return Map.of();
    }
}

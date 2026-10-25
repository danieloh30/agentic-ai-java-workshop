package com.incidentmanagement.agentic.workflow;

import com.incidentmanagement.agentic.agents.ReportCriticAgent;
import com.incidentmanagement.agentic.agents.ReportDrafterAgent;
import com.incidentmanagement.model.ReportCritique;

import dev.langchain4j.agentic.declarative.BeforeCall;
import dev.langchain4j.agentic.declarative.ExitCondition;
import dev.langchain4j.agentic.declarative.LoopAgent;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.service.MemoryId;

import io.quarkus.logging.Log;

import java.util.Map;

/** A fixed draft-then-critique loop; each report request has independent model context. */
public interface IncidentReportWorkflow {
    // TODO Ex8: Set the loop budget to three draft-and-critique iterations.
    @LoopAgent(
            name = "incident-report-quality-loop",
            outputKey = "reportResult",
            subAgents = {ReportDrafterAgent.class, ReportCriticAgent.class},
            maxIterations = 1)
    Map<String, Object> generateReport(
            @MemoryId String reportInvocationId,
            String system,
            String service,
            String priority,
            String description,
            String status);

    @BeforeCall
    static void initialize(AgenticScope scope) {
        // An empty critique starts the first draft without fabricated reviewer feedback.
        scope.writeState("critique", new ReportCritique(0, ""));
    }

    @ExitCondition(testExitAtLoopEnd = true)
    static boolean qualitySatisfied(AgenticScope scope) {
        ReportCritique critique = scope.readState("critique", (ReportCritique) null);
        Log.infof(
                "Report Critique -- iteration %d: score=%d",
                scope.agentInvocations(ReportCriticAgent.class).size(), critique.score());
        // TODO Ex8: Continue until the score reaches 7, within the annotation's budget.
        return true;
    }

    @Output
    static Map<String, Object> result(AgenticScope scope) {
        ReportCritique critique = scope.readState("critique", (ReportCritique) null);
        return Map.of(
                "report",
                scope.readState("report", ""),
                "score",
                critique.score(),
                "feedback",
                critique.feedback() == null ? "" : critique.feedback(),
                "iteration",
                scope.agentInvocations(ReportCriticAgent.class).size());
    }
}

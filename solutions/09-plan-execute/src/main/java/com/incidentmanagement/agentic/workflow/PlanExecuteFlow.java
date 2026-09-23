package com.incidentmanagement.agentic.workflow;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.incidentmanagement.agentic.agents.CommunicationAgent;
import com.incidentmanagement.agentic.agents.DiagnosisAgent;
import com.incidentmanagement.agentic.agents.MitigationAgent;
import com.incidentmanagement.agentic.agents.PlannerAgent;
import com.incidentmanagement.agentic.agents.ResolutionReviewAgent;
import com.incidentmanagement.agentic.agents.VerificationAgent;
import com.incidentmanagement.model.ExecutionPlan;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.ResolutionReview;
import com.incidentmanagement.model.ResolutionStep;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import io.quarkus.logging.Log;

/**
 * Plan &amp; Execute, built by hand on the {@code AgenticServices} loop you met in Exercise 8.
 *
 * <p>Three actions share one {@code AgenticScope} and repeat until the reviewer is satisfied:
 * <ol>
 *   <li><b>Plan</b> — {@link PlannerAgent} turns the incident (plus any reviewer feedback)
 *       into an ordered {@link ExecutionPlan} of specialist steps.</li>
 *   <li><b>Execute</b> — each planned step that has not run yet calls its specialist and
 *       writes the result into the scope.</li>
 *   <li><b>Review</b> — {@link ResolutionReviewAgent} decides whether the incident is
 *       resolved; if not, its feedback drives a re-plan on the next iteration.</li>
 * </ol>
 * Unlike a fixed sequence, the <em>set and order</em> of specialists is chosen by the LLM
 * at run time — that is what makes this Plan &amp; Execute rather than a static workflow.
 */
@ApplicationScoped
public class PlanExecuteFlow {

    private static final int MAX_ITERATIONS = 3;

    @Inject PlannerAgent plannerAgent;
    @Inject ResolutionReviewAgent reviewAgent;
    @Inject DiagnosisAgent diagnosisAgent;
    @Inject MitigationAgent mitigationAgent;
    @Inject VerificationAgent verificationAgent;
    @Inject CommunicationAgent communicationAgent;

    public Map<String, Object> resolve(IncidentInfo incident, Integer incidentNumber, String report) {

        var planAction = AgenticServices.agentAction(scope -> {
            String feedback = scope.readState("feedback", "");
            int iteration = scope.readState("iteration", 0) + 1;
            scope.writeState("iteration", iteration);

            ExecutionPlan plan = plannerAgent.plan(
                    incident.system, incident.service, incident.priority,
                    incident.description != null ? incident.description : "",
                    incidentNumber, report != null ? report : "", feedback);
            scope.writeState("plan", plan);
            Log.infof("Plan (iteration %d): %s — %s", iteration, plan.steps(), plan.rationale());
        });

        var executeAction = AgenticServices.agentAction(scope -> {
            ExecutionPlan plan = scope.readState("plan", null);
            if (plan == null || plan.steps() == null) {
                return;
            }
            // Run exactly what the current plan asks for. On a re-plan the planner only
            // schedules the steps still needed, so re-running them here is the point —
            // it produces fresh results for the reviewer to judge.
            Set<ResolutionStep> planned = new LinkedHashSet<>(plan.steps());
            for (ResolutionStep step : planned) {
                Log.infof("Execute: %s", step);
                switch (step) {
                    case DIAGNOSE -> scope.writeState("diagnosis",
                            diagnosisAgent.diagnose(incident, incidentNumber, report != null ? report : ""));
                    case MITIGATE -> scope.writeState("mitigation",
                            mitigationAgent.mitigate(incident, incidentNumber, scope.readState("diagnosis", "")));
                    case VERIFY -> scope.writeState("verification",
                            verificationAgent.verify(incident, incidentNumber,
                                    scope.readState("diagnosis", ""), scope.readState("mitigation", "")));
                    case COMMUNICATE -> scope.writeState("communication",
                            communicationAgent.communicate(incident, incidentNumber,
                                    scope.readState("diagnosis", ""), scope.readState("mitigation", ""),
                                    scope.readState("verification", "")));
                }
            }
        });

        var reviewAction = AgenticServices.agentAction(scope -> {
            String diagnosis = scope.readState("diagnosis", "");
            String mitigation = scope.readState("mitigation", "");
            String verification = scope.readState("verification", "");
            String communication = scope.readState("communication", "");

            ResolutionReview review = reviewAgent.review(
                    incident.system, incident.service, incident.priority, incidentNumber,
                    diagnosis, mitigation, verification, communication);

            scope.writeState("resolved", review.resolved());
            scope.writeState("feedback", """
                    Reviewer verdict: %s
                    Diagnosis so far: %s
                    Mitigation so far: %s
                    Verification so far: %s""".formatted(
                    review.feedback(),
                    diagnosis.isBlank() ? "(none)" : diagnosis,
                    mitigation.isBlank() ? "(none)" : mitigation,
                    verification.isBlank() ? "(none)" : verification));
            Log.infof("Review: resolved=%b — %s", review.resolved(), review.feedback());
        });

        UntypedAgent workflow = AgenticServices.loopBuilder()
                .name("incident-plan-execute-loop")
                .maxIterations(MAX_ITERATIONS)
                .exitCondition((scope, iteration) -> scope.readState("resolved", false))
                .subAgents(planAction, executeAction, reviewAction)
                .build();

        var result = workflow.invokeWithAgenticScope(Map.of());
        return result.agenticScope().state();
    }
}

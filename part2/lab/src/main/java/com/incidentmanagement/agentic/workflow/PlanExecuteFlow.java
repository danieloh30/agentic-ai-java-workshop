package com.incidentmanagement.agentic.workflow;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.incidentmanagement.evidence.DeploymentEvidenceClient;

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
 *   <li><b>Execute</b> — each step requested by the current plan calls its specialist and
 *       writes the result into the scope.</li>
 *   <li><b>Review</b> — {@link ResolutionReviewAgent} decides whether the recommendation is
 *       ready; if not, its feedback drives a re-plan on the next iteration.</li>
 * </ol>
 * Unlike a fixed sequence, the <em>set and order</em> of specialists is chosen by the LLM
 * at run time — that is what makes this Plan &amp; Execute rather than a static workflow.
 */
@ApplicationScoped
public class PlanExecuteFlow {

    private static final int MAX_ITERATIONS = 3;

    @Inject DeploymentEvidenceClient evidenceClient;
    @Inject PlannerAgent plannerAgent;
    @Inject ResolutionReviewAgent reviewAgent;
    @Inject DiagnosisAgent diagnosisAgent;
    @Inject MitigationAgent mitigationAgent;
    @Inject VerificationAgent verificationAgent;
    @Inject CommunicationAgent communicationAgent;

    public Map<String, Object> resolve(IncidentInfo incident, Integer incidentNumber, String report) {

        // TODO Ex10: replace this explicit no-lookup baseline with evidenceClient.fetch(incident.system).
        var evidence = new com.incidentmanagement.evidence.DeploymentEvidence(
                incident.system, "not_requested", "Not fetched", "No deployment evidence was fetched; use only incident facts.");
        if (!evidence.usableFor(incident.system) && !"not_requested".equals(evidence.status())) {
            return Map.of("outcome", "EVIDENCE_UNAVAILABLE", "recommendationReady", false,
                    "iteration", 0, "evidence", evidence, "history", List.of());
        }
        List<Map<String, Object>> history = new ArrayList<>();
        var planAction = AgenticServices.agentAction(scope -> {
            String feedback = scope.readState("feedback", "");
            int iteration = scope.readState("iteration", 0) + 1;
            scope.writeState("iteration", iteration);

            ExecutionPlan plan = plannerAgent.plan(
                    incident.system, incident.service, incident.priority,
                    incident.description != null ? incident.description : "",
                    incidentNumber, report != null ? report : "", evidence.toString(), feedback);
            scope.writeState("plan", plan);
            Log.infof("Planned response for incident #%d, iteration %d", incidentNumber, iteration);
        });

        var executeAction = AgenticServices.agentAction(scope -> {
            ExecutionPlan plan = scope.readState("plan", null);
            try {
                PlanValidator.validate(plan, !scope.readState("diagnosis", "").isBlank(),
                        !scope.readState("mitigation", "").isBlank());
                scope.writeState("invalidPlan", false);
            } catch (IllegalArgumentException invalid) {
                scope.writeState("invalidPlan", true);
                scope.writeState("recommendationReady", false);
                scope.writeState("feedback", "Plan rejected before execution: " + invalid.getMessage());
                return;
            }
            // Run exactly what the current plan asks for. On a re-plan the planner only
            // schedules the steps still needed, so re-running them here is the point —
            // it produces fresh results for the reviewer to judge.
            Set<ResolutionStep> planned = new LinkedHashSet<>(plan.steps());
            for (ResolutionStep step : planned) {
                Log.infof("Execute: %s", step);
                switch (step) {
                    case DIAGNOSE -> {
                        scope.writeState("mitigation", "");
                        scope.writeState("verification", "");
                        scope.writeState("diagnosis", diagnosisAgent.diagnose(incident, incidentNumber,
                                report != null ? report : "", evidence.toString()));
                    }
                    case MITIGATE -> {
                        scope.writeState("verification", "");
                        scope.writeState("mitigation", mitigationAgent.mitigate(incident, incidentNumber,
                                scope.readState("diagnosis", "")));
                    }
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
            if (scope.readState("invalidPlan", false)) {
                history.add(Map.of("iteration", scope.readState("iteration", 0),
                        "plan", scope.readState("plan", new ExecutionPlan(List.of(), "")),
                        "feedback", scope.readState("feedback", ""), "accepted", false));
                return;
            }
            String diagnosis = scope.readState("diagnosis", "");
            String mitigation = scope.readState("mitigation", "");
            String verification = scope.readState("verification", "");
            String communication = scope.readState("communication", "");

            ResolutionReview review = reviewAgent.review(
                    incident.system, incident.service, incident.priority, incidentNumber,
                    diagnosis, mitigation, verification, communication);

            scope.writeState("recommendationReady", review.recommendationReady());
            scope.writeState("feedback", """
                    Reviewer verdict: %s
                    Diagnosis so far: %s
                    Mitigation so far: %s
                    Verification so far: %s""".formatted(
                    review.feedback(),
                    diagnosis.isBlank() ? "(none)" : diagnosis,
                    mitigation.isBlank() ? "(none)" : mitigation,
                    verification.isBlank() ? "(none)" : verification));
            history.add(Map.of("iteration", scope.readState("iteration", 0), "plan", scope.readState("plan", null),
                    "feedback", review.feedback(), "accepted", true, "recommendationReady", review.recommendationReady()));
            Log.infof("Review for incident #%d: recommendationReady=%b", incidentNumber, review.recommendationReady());
        });

        UntypedAgent workflow = AgenticServices.loopBuilder()
                .name("incident-plan-execute-loop")
                .maxIterations(MAX_ITERATIONS)
                .exitCondition((scope, iteration) -> scope.readState("recommendationReady", false))
                .subAgents(planAction, executeAction, reviewAction)
                .build();

        var result = workflow.invokeWithAgenticScope(Map.of());
        var state = new java.util.LinkedHashMap<String, Object>(result.agenticScope().state());
        state.put("evidence", evidence);
        state.put("history", history);
        state.put("outcome", Boolean.TRUE.equals(state.get("recommendationReady"))
                ? "RECOMMENDATION_READY" : "NEEDS_OPERATOR_REVIEW");
        return state;
    }
}

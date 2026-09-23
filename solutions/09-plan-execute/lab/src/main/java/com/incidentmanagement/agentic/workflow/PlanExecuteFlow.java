package com.incidentmanagement.agentic.workflow;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.incidentmanagement.agentic.agents.CommunicationAgent;
import com.incidentmanagement.agentic.agents.DiagnosisAgent;
import com.incidentmanagement.agentic.agents.MitigationAgent;
import com.incidentmanagement.agentic.agents.PlannerAgent;
import com.incidentmanagement.agentic.agents.ResolutionReviewAgent;
import com.incidentmanagement.agentic.agents.VerificationAgent;
import com.incidentmanagement.model.IncidentInfo;

/**
 * Plan &amp; Execute, built by hand on the {@code AgenticServices} loop you met in Exercise 8.
 *
 * <p>TODO (Exercise 9): implement {@link #resolve} as a three-action loop over one shared
 * {@code AgenticScope}, using {@code dev.langchain4j.agentic.AgenticServices}:
 * <ol>
 *   <li><b>planAction</b> — read {@code "feedback"} from the scope (empty on the first pass),
 *       call {@link #plannerAgent}{@code .plan(...)}, and write the {@code "plan"}.</li>
 *   <li><b>executeAction</b> — for each {@code ResolutionStep} in the current plan, call the
 *       matching specialist and write its output ({@code "diagnosis"}, {@code "mitigation"},
 *       {@code "verification"}, {@code "communication"}) into the scope.</li>
 *   <li><b>reviewAction</b> — call {@link #reviewAgent}{@code .review(...)}, then write
 *       {@code "resolved"} and a {@code "feedback"} summary the planner can re-plan from.</li>
 * </ol>
 * Wire them with {@code AgenticServices.loopBuilder()}, {@code .maxIterations(3)}, and an
 * {@code .exitCondition((scope, i) -> scope.readState("resolved", false))}, then
 * {@code invokeWithAgenticScope(Map.of())} and return {@code result.agenticScope().state()}.
 */
@ApplicationScoped
public class PlanExecuteFlow {

    @Inject PlannerAgent plannerAgent;
    @Inject ResolutionReviewAgent reviewAgent;
    @Inject DiagnosisAgent diagnosisAgent;
    @Inject MitigationAgent mitigationAgent;
    @Inject VerificationAgent verificationAgent;
    @Inject CommunicationAgent communicationAgent;

    public Map<String, Object> resolve(IncidentInfo incident, Integer incidentNumber, String report) {
        // TODO: build and run the plan -> execute -> review loop (see the class Javadoc above).
        throw new UnsupportedOperationException("TODO: implement the Plan & Execute loop (Exercise 9)");
    }
}

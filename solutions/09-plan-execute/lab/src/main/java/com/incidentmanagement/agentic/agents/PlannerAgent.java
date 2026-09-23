package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.ExecutionPlan;

import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The "Plan" half of Plan &amp; Execute. This LLM planner does not resolve the incident
 * itself — it decides <em>which</em> specialists to run and <em>in what order</em>,
 * returning a structured {@link ExecutionPlan} the {@code PlanExecuteFlow} then carries out.
 *
 * <p>TODO (Exercise 9): make this a working planner.
 * <ol>
 *   <li>Add a {@code @SystemMessage} describing the planner's role and the available steps
 *       (DIAGNOSE, MITIGATE, VERIFY, COMMUNICATE), the ordering rules (diagnose before
 *       mitigate before verify; communicate for high-priority incidents), and — importantly —
 *       that when {@code feedback} is present it should plan ONLY the additional steps still
 *       needed (this is what enables dynamic re-planning).</li>
 *   <li>Add a {@code @UserMessage} that passes the incident facts and the {@code {feedback}}
 *       placeholder.</li>
 * </ol>
 * Returning the {@link ExecutionPlan} record gives you structured output for free.
 */
@ApplicationScoped
@RegisterAiService
public interface PlannerAgent {

    // TODO: annotate with @SystemMessage and @UserMessage (see the class Javadoc above).
    ExecutionPlan plan(String system, String service, String priority, String description,
                       Integer incidentNumber, String report, String feedback);
}

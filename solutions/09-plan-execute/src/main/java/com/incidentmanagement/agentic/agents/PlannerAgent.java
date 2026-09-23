package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.ExecutionPlan;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The "Plan" half of Plan &amp; Execute. This LLM planner does not do the work itself —
 * it decides <em>which</em> specialists to run and <em>in what order</em>, returning a
 * structured {@link ExecutionPlan} the executor then carries out.
 *
 * <p>On the first pass, {@code feedback} is empty and the planner builds an initial plan
 * from the incident facts. On a re-plan, {@code feedback} carries the reviewer's verdict
 * and the work done so far, so the planner can append the steps still needed — this is
 * the "dynamic re-planning" step of the pattern.
 */
@ApplicationScoped
@RegisterAiService
public interface PlannerAgent {

    @SystemMessage("""
            You are the planning agent for an IT incident-management system.
            You do NOT resolve the incident yourself. You decide which specialist steps to
            run, and in what order, then hand that plan off to an executor.

            The available steps are:
              - DIAGNOSE:    investigate and identify the most likely root cause
              - MITIGATE:    take a concrete action to reduce or remove impact (needs a diagnosis first)
              - VERIFY:      confirm whether the incident now appears resolved (needs mitigation first)
              - COMMUNICATE: write a stakeholder status update

            Rules:
              - Order steps so each has what it needs: DIAGNOSE before MITIGATE before VERIFY.
              - Include COMMUNICATE for high-priority incidents (P1/P2); it is optional for low ones.
              - Do not repeat a step that the feedback shows is already done and adequate.
              - If feedback is provided, plan ONLY the additional steps still needed to resolve it.
              - Keep the rationale to a single sentence.
            """)
    @UserMessage("""
            Incident: {system}/{service} (priority {priority}, #{incidentNumber})
            Description: {description}
            Operator report: {report}

            Work done so far / reviewer feedback (empty on the first plan):
            {feedback}
            """)
    ExecutionPlan plan(String system, String service, String priority, String description,
                       Integer incidentNumber, String report, String feedback);
}

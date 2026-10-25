package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.ExecutionPlan;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.agentic.Agent;

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
public interface PlanGenerationAgent {

    @SystemMessage("""
            You plan IT incident response; you do not execute production changes.
            Available specialist steps:
            DIAGNOSE: identify the likely cause using supplied facts and deployment evidence.
            MITIGATE: recommend a mitigation; requires a diagnosis.
            VERIFY: assess the recommendation and name checks an operator must perform;
                    requires mitigation. Do not claim these checks have actually run.
            COMMUNICATE: write a stakeholder update; include this for P1/P2 incidents.

            Order DIAGNOSE before MITIGATE before VERIFY.
            On replanning, use feedback and prior results; schedule only unfinished work.
            Never invent deployments, metrics, tool results, or completed actions.
            Return the ordered steps and a one-sentence rationale.
            """)
    @UserMessage("""
            Incident: {system}/{service} (priority {priority}, #{incidentNumber})
            Description: {description}
            Operator report: {report}
            Required deployment evidence (workshop fixture): {evidence}

            Work done so far / reviewer feedback (empty on the first plan):
            {feedback}
            """)
    @Agent(description = "Builds a structured specialist plan using incident facts and retrieved evidence.",
           outputKey = "plan")
    ExecutionPlan plan(String system, String service, String priority, String description,
                       Integer incidentNumber, String report, String evidence, String feedback);
}

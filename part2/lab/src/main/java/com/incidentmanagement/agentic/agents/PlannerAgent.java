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
public interface PlannerAgent {

    // TODO Ex9: replace this runnable prompt with the ordering and replanning rules in the guide.
    @SystemMessage("""
            Plan incident response using DIAGNOSE, MITIGATE, VERIFY, COMMUNICATE.
            Return an ordered plan and rationale. Use only supplied facts and evidence.
            Never claim to have performed changes or observed recovery.
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

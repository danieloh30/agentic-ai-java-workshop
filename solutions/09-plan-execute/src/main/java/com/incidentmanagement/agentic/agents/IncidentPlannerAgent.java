package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.declarative.PlannerAgent;

/**
 * Plan &amp; Execute, the declarative way.
 *
 * <p>{@code @PlannerAgent} is the framework's planning orchestrator (a sibling of
 * {@code @SupervisorAgent}). Given its {@code subAgents} and their descriptions, an
 * LLM planner pre-computes a plan of which specialists to call and in what order,
 * executes them through a shared {@code AgenticScope}, and re-plans until the goal
 * is reached — exactly the "Plan &amp; Execute (DAG decomposition + dynamic
 * re-planning)" pattern, with no hand-written loop.
 */
public interface IncidentPlannerAgent {

    @PlannerAgent(
            outputKey = "planResult",
            subAgents = {
                    DiagnosisAgent.class,
                    MitigationAgent.class,
                    VerificationAgent.class,
                    CommunicationAgent.class
            })
    String resolveIncident(IncidentInfo incidentInfo, Integer incidentNumber, String report);
}

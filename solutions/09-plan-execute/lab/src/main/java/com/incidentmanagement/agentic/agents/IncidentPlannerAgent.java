package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.declarative.PlannerAgent;

/**
 * Plan &amp; Execute, the declarative way — the star of this exercise.
 *
 * <p>{@code @PlannerAgent} is the framework's planning orchestrator (a sibling of
 * {@code @SupervisorAgent} from Exercise 4). You give it sub-agents; an LLM planner
 * decides which to call and in what order, executes them through a shared scope, and
 * re-plans until the goal is met.
 */
public interface IncidentPlannerAgent {

    // TODO Exercise 09 — Step 3: annotate this method with @PlannerAgent.
    // Set outputKey = "planResult" and list all four specialists in subAgents:
    // DiagnosisAgent, MitigationAgent, VerificationAgent, CommunicationAgent.
    // See docs/09-plan-execute/START_HERE.md. The method signature stays as-is.
    //
    // @PlannerAgent(
    //         outputKey = "planResult",
    //         subAgents = { DiagnosisAgent.class, MitigationAgent.class,
    //                       VerificationAgent.class, CommunicationAgent.class })
    String resolveIncident(IncidentInfo incidentInfo, Integer incidentNumber, String report);
}

package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;

/**
 * A specialist the planner can call: take an action that reduces or removes impact.
 * A good {@code description} tells the planner this depends on a diagnosis, so it
 * schedules the two in the right order.
 */
public interface MitigationAgent {

    // TODO Exercise 09 — Step 2: turn this into an @Agent specialist.
    // Add @SystemMessage (mitigation persona), @UserMessage (the incident facts +
    // {diagnosis}), and @Agent(description = "...", outputKey = "mitigation").
    // Hint the ordering in the description (e.g. "Best run after a diagnosis exists.").
    // See docs/09-plan-execute/START_HERE.md. Keep the method signature.
    String mitigate(IncidentInfo incidentInfo, Integer incidentNumber, String diagnosis);
}

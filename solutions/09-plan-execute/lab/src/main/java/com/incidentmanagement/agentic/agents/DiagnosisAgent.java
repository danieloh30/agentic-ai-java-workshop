package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;

/**
 * A specialist the planner can call: investigate and identify root cause.
 * The {@code description} you give its {@code @Agent} annotation is how the planner
 * decides when to schedule this step — so write it for the planner to read.
 */
public interface DiagnosisAgent {

    // TODO Exercise 09 — Step 1: turn this into an @Agent specialist.
    // Add @SystemMessage (diagnosis persona), @UserMessage (the incident facts +
    // {report}), and @Agent(description = "...", outputKey = "diagnosis").
    // The description is what the planner uses to decide to call you.
    // See docs/09-plan-execute/START_HERE.md. Keep the method signature.
    String diagnose(IncidentInfo incidentInfo, Integer incidentNumber, String report);
}

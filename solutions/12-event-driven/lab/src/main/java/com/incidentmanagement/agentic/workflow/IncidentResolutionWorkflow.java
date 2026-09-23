package com.incidentmanagement.agentic.workflow;

import com.incidentmanagement.model.IncidentEvent;
import com.incidentmanagement.model.ResolutionEvent;

import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * The agentic pipeline that runs for each incoming event: triage, then resolve. (Exercise 12, Step 1)
 *
 * <p>The two stages ({@code TriageAgent}, {@code ResolutionAgent}) are written for you. Your job
 * is to chain them with a declarative {@code @SequenceAgent} and assemble their outputs into a
 * {@link ResolutionEvent}.
 *
 * <p>TODO (Step 1):
 *   1. Annotate {@code process(...)} with
 *      {@code @SequenceAgent(outputKey = "resolutionResult",
 *              subAgents = { TriageAgent.class, ResolutionAgent.class })}.
 *      TriageAgent writes "triage" into the scope; ResolutionAgent reads it and writes "resolution".
 *   2. Implement the {@code @Output output(AgenticScope scope)} method: read "incidentEvent",
 *      "triage" and "resolution" from the scope and build a {@link ResolutionEvent}.
 *
 * Reading from the scope:
 *   IncidentEvent event = scope.readState("incidentEvent", (IncidentEvent) null);
 *   String triage = scope.readState("triage", "");
 *
 * Imports you'll need:
 *   import com.incidentmanagement.agentic.agents.TriageAgent;
 *   import com.incidentmanagement.agentic.agents.ResolutionAgent;
 *   import dev.langchain4j.agentic.declarative.Output;
 *   import dev.langchain4j.agentic.declarative.SequenceAgent;
 */
public interface IncidentResolutionWorkflow {

    // TODO Step 1.1: add @SequenceAgent(outputKey = "resolutionResult", subAgents = { ... })
    ResolutionEvent process(IncidentEvent incidentEvent);

    // TODO Step 1.2: add @Output above this method and build the ResolutionEvent from the scope.
    static ResolutionEvent output(AgenticScope scope) {
        throw new UnsupportedOperationException("TODO: assemble the ResolutionEvent (Exercise 12)");
    }
}

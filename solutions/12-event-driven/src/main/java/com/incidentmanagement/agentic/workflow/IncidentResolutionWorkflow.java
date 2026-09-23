package com.incidentmanagement.agentic.workflow;

import com.incidentmanagement.agentic.agents.ResolutionAgent;
import com.incidentmanagement.agentic.agents.TriageAgent;
import com.incidentmanagement.model.IncidentEvent;
import com.incidentmanagement.model.ResolutionEvent;

import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * The agentic pipeline that runs for each incoming event: triage, then resolve.
 *
 * <p>A declarative {@code @SequenceAgent} chains the two {@code @Agent}s through a shared
 * scope — {@link TriageAgent} writes {@code triage}, {@link ResolutionAgent} reads it and
 * writes {@code resolution}. The {@code @Output} method then assembles both, plus the
 * incident id, into the {@link ResolutionEvent} that gets published downstream.
 *
 * <p>This is the same composition pattern you met in Exercises 2–4; here it's driven by a
 * Kafka event instead of an HTTP request. The agents don't know or care what triggered them.
 */
public interface IncidentResolutionWorkflow {

    @SequenceAgent(
            outputKey = "resolutionResult",
            subAgents = {
                    TriageAgent.class,
                    ResolutionAgent.class
            })
    ResolutionEvent process(IncidentEvent incidentEvent);

    @Output
    static ResolutionEvent output(AgenticScope scope) {
        IncidentEvent event = scope.readState("incidentEvent", (IncidentEvent) null);
        String triage = scope.readState("triage", "");
        String resolution = scope.readState("resolution", "");
        return new ResolutionEvent(event != null ? event.incidentId() : null, triage, resolution);
    }
}

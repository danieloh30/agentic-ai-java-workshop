package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentEvent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * First stage of the event-driven pipeline: classify the incident.
 *
 * <p>A declarative {@code @Agent} so the {@code @SequenceAgent} workflow can chain it before
 * the resolver. Its output lands in the shared scope under {@code outputKey = "triage"},
 * which the {@link ResolutionAgent} then reads via {@code {triage}}.
 */
public interface TriageAgent {

    @SystemMessage("""
            You are the triage stage of an automated incident pipeline. In 2–3 sentences,
            classify the incident: the most likely category (capacity, regression, dependency,
            configuration, or data), the apparent severity, and the single most important signal
            to check next. Be concise and concrete — this is a machine-to-machine handoff.
            """)
    @UserMessage("""
            Incident: {incidentEvent.system}/{incidentEvent.service} (priority P{incidentEvent.priority})
            Description: {incidentEvent.description}
            Operator report: {incidentEvent.report}
            """)
    @Agent(description = "Classifies an incident event: likely category, severity, and next signal to check.",
           outputKey = "triage")
    String triage(IncidentEvent incidentEvent);
}

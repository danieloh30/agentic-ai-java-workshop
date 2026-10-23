package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentEvent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Second stage of the event-driven pipeline: recommend a resolution.
 *
 * <p>Reads the previous stage's classification from the shared scope via {@code {triage}}
 * (its {@code outputKey}) and produces the final recommendation. Its output lands under
 * {@code outputKey = "resolution"}, which is what the {@code @SequenceAgent} returns.
 */
public interface ResolutionAgent {

    @SystemMessage("""
            You are the resolution stage of an automated incident pipeline. Given the incident
            and the triage classification, recommend a concrete next action and a one-line
            justification. Start with a single action verb on its own line: ROLLBACK, SCALE_UP,
            RESTART, or MONITOR. Then give at most three sentences of guidance. Do not invent
            metrics or logs that were not provided.
            """)
    @UserMessage("""
            Incident: {incidentEvent.system}/{incidentEvent.service} (priority P{incidentEvent.priority})
            Description: {incidentEvent.description}
            Operator report: {incidentEvent.report}

            Triage classification:
            {triage}
            """)
    @Agent(description = "Recommends a concrete resolution for an incident given its triage classification.",
           outputKey = "resolution")
    String resolve(IncidentEvent incidentEvent, String triage);
}

package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * A specialist the planner can call: take an action that reduces or removes impact.
 * The planner will normally schedule this after {@link DiagnosisAgent}.
 */
public interface MitigationAgent {

    @SystemMessage("""
            You are a mitigation specialist for an IT incident-management system.
            Given the incident and its diagnosis, state the single concrete mitigating
            action to take and its expected effect. Do not claim work is already done that
            you were not asked to do. Keep it to 2–4 sentences.
            """)
    @UserMessage("""
            Incident: {incidentInfo.system}/{incidentInfo.service} (P{incidentInfo.priority}, #{incidentNumber})
            Description: {incidentInfo.description}

            Diagnosis (if available): {diagnosis}
            """)
    @Agent(description = "Takes a concrete action to reduce or remove the incident's impact. Best run after a diagnosis exists.",
           outputKey = "mitigation")
    String mitigate(IncidentInfo incidentInfo, Integer incidentNumber, String diagnosis);
}

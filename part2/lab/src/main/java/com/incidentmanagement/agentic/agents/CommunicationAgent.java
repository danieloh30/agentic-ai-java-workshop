package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * A specialist the planner can call: notify stakeholders / update the record.
 * Whether and when this runs is the planner's decision — a P1 needs it, a P4 may not.
 */
public interface CommunicationAgent {

    @SystemMessage("""
            You are a stakeholder-communication specialist for an IT incident-management
            system. Produce a short status update suitable for stakeholders, based only on
            what is known about the incident and its handling. Keep it to 2–3 sentences.
            """)
    @UserMessage("""
            Incident: {incidentInfo.system}/{incidentInfo.service} (P{incidentInfo.priority}, #{incidentNumber})
            Description: {incidentInfo.description}

            Diagnosis (if available): {diagnosis}
            Mitigation (if available): {mitigation}
            Verification (if available): {verification}
            """)
    @Agent(description = "Writes a stakeholder status update. Useful for high-priority incidents.",
           outputKey = "communication")
    String communicate(IncidentInfo incidentInfo, Integer incidentNumber,
                       String diagnosis, String mitigation, String verification);
}

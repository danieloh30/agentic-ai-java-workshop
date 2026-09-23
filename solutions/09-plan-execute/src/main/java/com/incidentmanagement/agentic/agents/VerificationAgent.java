package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * A specialist the planner can call: confirm the incident is actually resolved.
 * The planner will normally schedule this after {@link MitigationAgent}.
 */
public interface VerificationAgent {

    @SystemMessage("""
            You are a verification specialist for an IT incident-management system.
            Given the diagnosis and the mitigation taken, state whether the incident now
            appears resolved and how you would confirm it (the check to run / signal to
            watch). Be honest if verification is inconclusive. Keep it to 2–4 sentences.
            """)
    @UserMessage("""
            Incident: {incidentInfo.system}/{incidentInfo.service} (P{incidentInfo.priority}, #{incidentNumber})

            Diagnosis (if available): {diagnosis}
            Mitigation (if available): {mitigation}
            """)
    @Agent(description = "Confirms whether the incident is resolved after mitigation. Run last.",
           outputKey = "verification")
    String verify(IncidentInfo incidentInfo, Integer incidentNumber, String diagnosis, String mitigation);
}

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
            Given the diagnosis and the mitigation taken, decide whether the mitigation
            addresses the diagnosed root cause.

            Start your answer with a one-word verdict on its own: RESOLVED or NOT RESOLVED.
            - Say RESOLVED when the mitigation directly targets the root cause and there is no
              sign it failed; then name the check or signal that confirms it in production.
            - Say NOT RESOLVED only when the mitigation does not address the diagnosis, is
              missing, or there is evidence it did not work; then say what is still needed.
            Do not withhold a RESOLVED verdict merely because production monitoring has not
            been observed yet. Keep it to 2–4 sentences after the verdict.
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

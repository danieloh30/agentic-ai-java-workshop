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
            You review a proposed mitigation for an IT incident. No production actions
            or monitoring checks have been executed by this workflow.
            Assess whether the proposal addresses the diagnosis.
            Start with READY when the recommendation is appropriate and complete,
            or NEEDS_REVISION when prerequisites are missing or the proposal is unsuitable.
            Name concrete production checks that an operator must perform before closing
            the incident. Never claim those checks have run or that recovery was observed.
            Keep the explanation to 2-4 sentences.
            """)
    @UserMessage("""
            Incident: {incidentInfo.system}/{incidentInfo.service} (P{incidentInfo.priority}, #{incidentNumber})

            Diagnosis (if available): {diagnosis}
            Mitigation (if available): {mitigation}
            """)
    @Agent(description = "Assesses a proposed mitigation and lists production checks still required.",
           outputKey = "verification")
    String verify(IncidentInfo incidentInfo, Integer incidentNumber, String diagnosis, String mitigation);
}

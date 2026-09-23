package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * A specialist the planner can call: investigate and identify root cause.
 * The {@code description} is how the planner decides when to schedule this step.
 */
public interface DiagnosisAgent {

    @SystemMessage("""
            You are a diagnosis specialist for an IT incident-management system.
            Identify the single most likely root cause of the incident and the evidence
            for it. Use only the incident facts provided; do not invent metrics or logs.
            Keep it to 2–4 sentences.
            """)
    @UserMessage("""
            Incident: {incidentInfo.system}/{incidentInfo.service} (P{incidentInfo.priority}, #{incidentNumber})
            Description: {incidentInfo.description}
            Operator report: {report}
            """)
    @Agent(description = "Investigates the incident and identifies the most likely root cause.",
           outputKey = "diagnosis")
    String diagnose(IncidentInfo incidentInfo, Integer incidentNumber, String report);
}

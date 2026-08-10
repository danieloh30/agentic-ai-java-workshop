package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that analyzes an incident report to determine if triage is needed.
 */
public interface TriageFeedbackAgent {

    @SystemMessage("""
        You are a triage analyzer for an IT incident management system.
        Your job is to determine if an incident needs triage based on the report and current incident status.
        Diagnostic investigation never performs triage. If the incident needed triage prior to diagnostics, it will still need triage afterwards.
        If the report mentions service degradation, outages, errors, or any operational issues that suggest the incident needs triage, recommend triage.
        If there's a triage-related report, prioritize it over the previous incident description.
        Be specific about what type of triage is needed (assign on-call, notify stakeholders, create war room, link related incidents).
        If no triage actions are needed based on the report, respond with "TRIAGE_NOT_REQUIRED".
        Include the reason for your choice but keep your response short.
        """)
    @UserMessage("""
        Incident Information:
        System: {incidentInfo.system}
        Service: {incidentInfo.service}
        Priority: {incidentInfo.priority}
        Previous Description: {incidentInfo.description}

        Report: {report}
        """)
    @Agent(description = "Triage analyzer. Using report, determines if triage is needed.",
            outputKey = "triageRequest")
    String analyzeForTriage(
            IncidentInfo incidentInfo,
            Integer incidentNumber,
            String report);
}

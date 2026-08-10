package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that analyzes incident data to determine the final resolution status.
 */
public interface ResolutionAgent {

    @SystemMessage("""
        You are an incident resolution analyzer for an IT incident management system.
        Your job is to determine the current resolution status of an incident based on analysis from other agents.
        Analyze all recommendations and the previous incident description to provide an updated resolution description.
        Always provide a very short (no more than 200 characters) resolution description, even if there's minimal input.
        Do not add any headers or prefixes to your response.
        """)
    @UserMessage("""
            Incident Information:
            System: {incidentInfo.system}
            Service: {incidentInfo.service}
            Priority: {incidentInfo.priority}
            Previous Description: {incidentInfo.description}

            Analysis from other agents:
            Triage Recommendation: {triageRequest}
            Diagnostic Recommendation: {diagnosticRequest}
            """)
    @Agent(description = "Incident resolution analyzer. Determines the current resolution status based on analysis.",
            outputKey = "incidentResolution")
    String analyzeForResolution(
            IncidentInfo incidentInfo,
            Integer incidentNumber,
            String triageRequest,
            String diagnosticRequest);
}

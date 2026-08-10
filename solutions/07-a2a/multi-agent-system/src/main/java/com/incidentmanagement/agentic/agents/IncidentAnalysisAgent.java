package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.AnalysisTask;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Unified agent that analyzes incidents based on the provided task configuration.
 * This agent is parameterized to handle severity, impact, and resolution analysis.
 */
public interface IncidentAnalysisAgent {

    @SystemMessage("{task.systemInstructions}")
    @UserMessage("""
        Incident Information:
        System: {incidentInfo.system}
        Service: {incidentInfo.service}
        Priority: P{incidentInfo.priority}
        Current Description: {incidentInfo.description}

        Report: {report}
        """)
    @Agent(description = "Incident analyzer. Using report, determines if action is needed based on task type.",
            outputKey = "incidentAnalysis")
    String analyzeIncident(
            AnalysisTask task,
            IncidentInfo incidentInfo,
            Integer incidentNumber,
            String report);
}

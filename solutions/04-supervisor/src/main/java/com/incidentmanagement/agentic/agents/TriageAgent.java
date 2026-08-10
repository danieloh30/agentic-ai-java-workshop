package com.incidentmanagement.agentic.agents;

import io.quarkiverse.langchain4j.ToolBox;

import com.incidentmanagement.agentic.tools.TriageTool;
import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

// --8<-- [start:triageAgent]
/**
 * Agent that determines what triage actions to take for an IT incident.
 */
public interface TriageAgent {

    @SystemMessage("""
        You handle intake for the triage department of an IT incident management system.
        It is your job to submit a request to the provided requestTriage function
        to take action based on the provided incident report.
        Be specific about what triage actions are needed.
        If no triage action is needed based on the report, respond with "TRIAGE_NOT_REQUIRED".
        """)
    @UserMessage("""
        Incident Information:
        System: {incidentInfo.system}
        Service: {incidentInfo.service}
        Priority: P{incidentInfo.priority}
        Incident Number: {incidentNumber}

        Report: {report}
        """)
    @Agent(description = "Triage specialist. Determines initial triage and team assignment.",
           outputKey = "analysisResult")
    @ToolBox(TriageTool.class)
    String processTriage(
            IncidentInfo incidentInfo,
            Integer incidentNumber,
            String report);
}
// --8<-- [end:triageAgent]

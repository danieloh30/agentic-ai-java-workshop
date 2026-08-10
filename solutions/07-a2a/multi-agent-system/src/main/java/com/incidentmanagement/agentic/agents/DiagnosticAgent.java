package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface DiagnosticAgent {

    @SystemMessage("""
        You handle intake for the diagnostic department of an IT incident management system.
        Based on the incident report, determine what specific diagnostic actions are needed
        and provide a detailed root cause analysis plan.
        Be specific about what actions are needed based on the incident report.

        Available diagnostic actions include:
        - Log analysis
        - Service restart
        - Config rollback
        - Dependency check
        - Performance profiling
        - Network trace (packet capture, DNS, connectivity)

        For infrastructure issues like connectivity or DNS problems, include network trace in your plan.

        Provide your response as a structured diagnostic plan listing the specific actions needed.
        If no diagnostic action is needed based on the request, respond with "DIAGNOSTIC_NOT_REQUIRED".
        """)
    @UserMessage("""
        Incident Information:
        System: {incidentInfo.system}
        Service: {incidentInfo.service}
        Priority: P{incidentInfo.priority}
        Incident Number: {incidentNumber}

        Diagnostic Request:
        {diagnosticRequest}
        """)
    @Agent(description = "Incident diagnostic specialist. Using incident information and request, determines what diagnostic actions are needed.",
           outputKey = "analysisResult")
    String processDiagnostic(IncidentInfo incidentInfo,
                              Integer incidentNumber,
                              String diagnosticRequest);
}

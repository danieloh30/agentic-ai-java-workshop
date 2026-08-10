package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that analyzes an incident report to determine if diagnostic investigation is needed.
 */
public interface DiagnosticFeedbackAgent {

    @SystemMessage("""
        You are a diagnostic analyzer for an IT incident management system.
        Your job is to determine if an incident needs diagnostic investigation based on the report.
        Analyze the report and incident information to decide if diagnostics are needed.
        Diagnostics never include any triage or team assignment actions.
        If the report mentions performance issues, errors, failures, resource exhaustion,
        or anything that suggests the incident needs diagnostic investigation, recommend appropriate diagnostics.
        Be specific about what type of diagnostics are needed (log analysis, service restart, config rollback,
        dependency check, performance profiling, network trace).
        If no diagnostics or investigation are needed, respond with "DIAGNOSTIC_NOT_REQUIRED".
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
    @Agent(description = "Diagnostic analyzer. Using report, determines if diagnostic investigation is needed.",
            outputKey = "diagnosticRequest")
    String analyzeForDiagnostic(
            IncidentInfo incidentInfo,
            Integer incidentNumber,
            String report);
}

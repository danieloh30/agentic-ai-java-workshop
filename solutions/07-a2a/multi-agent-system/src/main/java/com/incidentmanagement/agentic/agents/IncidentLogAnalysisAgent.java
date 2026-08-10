package com.incidentmanagement.agentic.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agent that analyzes an incident log screenshot and enriches the incident report with visual observations.
 * If no image is provided, the incident report is returned unchanged.
 */
public interface IncidentLogAnalysisAgent {

    @SystemMessage("""
        You are a log analysis specialist for an IT incident management system.
        You will optionally receive the current incident report for an active incident.
        If a screenshot of logs, dashboards, or error messages is provided, analyze it and rewrite the incident report taking count of
        your visual observations about the system's state (e.g., error patterns, stack traces, resource utilization,
        anomalous metrics, alert states, etc.).
        Avoid appending your visual observations in a separated section of the response, but combine
        the existing incident report, if present, with what you can see from the image in a single response.
        If no image is provided, or the image is empty or it doesn't seem related to IT systems,
        simply return the incident report exactly as it is, without any modification.
        Your response must always include the original incident report text, if present, followed by your observations.
        If no original incident report is provided, your response should only include your observations based on the image.
        In any cases the returned response MUST be a single sentence.
        """)
    @UserMessage("""
        Report: {report}
        """)
    @Agent(description = "Log analysis specialist. Enriches incident reports with visual observations from log screenshots.",
            outputKey = "report", optional = true)
    String analyzeIncidentLogs(String report, @UserMessage @V("logImage") ImageContent logImage);
}

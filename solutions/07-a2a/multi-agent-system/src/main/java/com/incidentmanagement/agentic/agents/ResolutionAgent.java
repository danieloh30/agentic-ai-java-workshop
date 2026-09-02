package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentOutcome;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.IncidentAnalysisResults;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ResolutionAgent {

    @SystemMessage("""
        Analyze incident processing results and output a JSON summary.

        Output format:
        {
          "resolution": "concise description (max 200 chars)",
          "incidentAction": "ESCALATE|INVESTIGATE|TRIAGE|MONITOR|RESOLVE"
        }

        Rules:
        - CRITICAL: The human approval decision OVERRIDES all other signals.
          If approvalDecision contains "RESOLVE_INCIDENT" → use RESOLVE.
          If approvalDecision contains "SKIPPED" → NEVER use ESCALATE; use INVESTIGATE or TRIAGE based on the analysis.
        - Only use ESCALATE if the human approved the escalation (approvalDecision contains "ESCALATE_INCIDENT" or "APPROVED").
        - If no human override applies, fall back to analysis:
          - If supervisorDecision mentions ESCALATE_P1/ASSIGN_TEAM (but NOT CLOSE) → ESCALATE
          - Else if resolutionAnalysis ≠ "ESCALATION_NOT_REQUIRED" → INVESTIGATE
          - Else if severityAnalysis ≠ "SEVERITY_LOW" → TRIAGE
          - Else if severityAnalysis = "SEVERITY_LOW" → MONITOR
          - Else → RESOLVE
        - resolution: Summarize the action and reason in plain language, mentioning the human decision if relevant
        """)
    @UserMessage("""
        Incident: P{incidentInfo.priority} {incidentInfo.system}/{incidentInfo.service} (#{incidentNumber})

        Supervisor Decision: {supervisorDecision}

        Human Approval Decision: {approvalDecision}

        Incident Analysis Results:
        - Resolution: {incidentAnalysisResults.resolutionAnalysis}
        - Impact: {incidentAnalysisResults.impactAnalysis}
        - Severity: {incidentAnalysisResults.severityAnalysis}
        """)
    @Agent(description = "Final incident resolution analyzer. Determines the incident's outcome and action based on all analysis.",
           outputKey = "incidentOutcome")
    IncidentOutcome analyzeForResolution(IncidentInfo incidentInfo, Integer incidentNumber,
                                          IncidentAnalysisResults incidentAnalysisResults,
                                          String supervisorDecision,
                                          String approvalDecision);
}

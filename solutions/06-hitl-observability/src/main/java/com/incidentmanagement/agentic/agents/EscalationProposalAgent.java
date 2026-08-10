package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.IncidentAnalysisResults;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface EscalationProposalAgent {

    @SystemMessage("""
        You are an escalation proposal specialist for an IT incident management system.
        Your role is to evaluate whether a critical incident should be escalated to executive management.
        Consider: incident priority (P1/P2), affected system criticality, business impact assessment, and estimated revenue loss.

        Escalation Options:
        - ESCALATE_TO_VP: Incident has severe business impact on revenue-critical systems
        - ESCALATE_TO_CTO: Incident involves critical infrastructure failure or security breach
        - KEEP_AT_TEAM_LEVEL: Incident can be handled by the current team

        Decision Criteria:
        - If revenue-critical system (payment, checkout, billing) with estimated revenue impact > $10,000: ESCALATE_TO_VP
        - If P1 on any system with high revenue impact: ESCALATE_TO_VP
        - If incident involves security breach, data loss, or infrastructure failure: ESCALATE_TO_CTO
        - If P3/P4 with low impact on non-critical system: KEEP_AT_TEAM_LEVEL
        - When in doubt for P1/P2 on critical systems, prefer ESCALATE over KEEP_AT_TEAM_LEVEL

        Your response must include:
        1. Proposed Action with unique marker: __ESCALATE_TO_VP__ or __ESCALATE_TO_CTO__ or __KEEP_AT_TEAM_LEVEL__
        2. Reasoning: Clear explanation of your recommendation

        Format your response as:
        Proposed Action: __[ESCALATE_TO_VP/ESCALATE_TO_CTO/KEEP_AT_TEAM_LEVEL]__
        Reasoning: [Your detailed explanation]

        CRITICAL: Use double underscores around the action (e.g., __ESCALATE_TO_VP__ not ESCALATE_TO_VP)
        """)
    @UserMessage("""
        Create an escalation proposal for this incident:
        - System: {incidentInfo.system}
        - Service: {incidentInfo.service}
        - Priority: P{incidentInfo.priority}
        - Incident Number: {incidentNumber}
        - Current Description: {incidentInfo.description}
        - Impact Analysis: {incidentAnalysisResults.impactAnalysis}
        - Resolution Analysis: {incidentAnalysisResults.resolutionAnalysis}
        - Incident Report: {report}

        Provide your escalation proposal with clear reasoning.
        """)
    @Agent(outputKey = "escalationProposal", description = "Creates escalation proposals for critical incidents requiring management attention")
    String createEscalationProposal(
            IncidentInfo incidentInfo,
            Integer incidentNumber,
            IncidentAnalysisResults incidentAnalysisResults,
            String report);
}

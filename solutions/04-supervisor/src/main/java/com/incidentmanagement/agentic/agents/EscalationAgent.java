package com.incidentmanagement.agentic.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface EscalationAgent {

    @SystemMessage("""
        You are an incident escalation specialist for an IT incident management system.
        Your job is to determine the best escalation action based on the incident's impact,
        severity, priority, and business consequences.

        Escalation Options:
        - ESCALATE_P1: Critical incident requiring VP/exec attention and war room
        - ASSIGN_TEAM: Route to specific engineering team for resolution
        - WORKAROUND: Apply temporary mitigation while root cause is investigated
        - CLOSE: Incident resolved or no action needed

        Decision Criteria:
        - If estimated revenue loss > $10,000/hr: Consider ESCALATE_P1 or ASSIGN_TEAM
        - If P1 with cascading failures: ESCALATE_P1
        - If P2 with contained impact: ASSIGN_TEAM
        - If workaround available and impact is temporary: WORKAROUND
        - If false alarm or already resolved: CLOSE

        Provide your recommendation with a clear explanation of the reasoning.
        """)
    @UserMessage("""
        Determine the escalation action for this incident:
        - System: {system}
        - Service: {service}
        - Priority: P{priority}
        - Incident Number: {incidentNumber}
        - Description: {incidentDescription}
        - Business Impact Assessment: {businessImpact}
        - Incident Report: {report}

        Provide your escalation recommendation (ESCALATE_P1/ASSIGN_TEAM/WORKAROUND/CLOSE) and explanation.
        """)
    @Agent(outputKey = "escalationAction",
           description = "Incident escalation specialist. Determines escalation path based on impact and severity.")
    String processEscalation(String system, String service, Integer priority,
                              Integer incidentNumber, String incidentDescription,
                              String businessImpact, String report);
}

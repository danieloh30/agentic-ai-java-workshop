package com.incidentmanagement.impact;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.cdi.spi.RegisterSimpleAgent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

@RegisterSimpleAgent(outputKey = "businessImpact")
public interface ImpactAgent {
    @SystemMessage("""
        You are Apex Systems' business impact assessment specialist supporting Riley, the SRE lead.
        Assess impact only; do not change incidents, approve escalation, or execute remediation.
        Revenue-critical services cost $10,000-$50,000 per hour of downtime;
        business-critical services $2,000-$10,000; supporting services $500-$2,000;
        non-critical services $100-$500. Full outages lose 100% of hourly cost,
        major degradation 60%, minor degradation 30%, and intermittent issues 20%.
        P1 has a 1-hour SLA, P2 4 hours, P3 8 hours, and P4 24 hours.
        Beyond the SLA, penalties per hour are 2x hourly cost for P1,
        1.5x for P2, 1x for P3, and 0.5x for P4.
        State assumptions when duration or financial evidence is missing. Do not invent evidence.
        Return exactly two lines: 'Estimated Impact: $XX,XXX' and
        'Justification: ...' with 2-3 sentences covering criticality, assumptions, and SLA risk.
        """)
    @UserMessage("""
        Assess this incident:
        System: {{system}}
        Service: {{service}}
        Priority: P{{priority}}
        Description: {{incidentDescription}}
        """)
    @Agent(description = "Estimates business impact and SLA cost for incident escalation decisions",
            outputKey = "businessImpact")
    String assessImpact(@V("system") String system, @V("service") String service,
            @V("priority") Integer priority, @V("incidentDescription") String incidentDescription);
}

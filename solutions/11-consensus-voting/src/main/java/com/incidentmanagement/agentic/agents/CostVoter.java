package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.Vote;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Voter #3 — the cost / operational-risk perspective.
 *
 * <p>Conservative by design: wary of expensive or disruptive actions taken on thin evidence.
 * Prefers to MONITOR or make the cheapest safe move, and only endorses a ROLLBACK or SCALE_UP
 * when the report clearly justifies the spend or risk. Its ballot lands in the shared scope
 * under {@code outputKey = "costVote"}.
 */
public interface CostVoter {

    @SystemMessage("""
            You are a platform engineer voting on how to remediate an incident, weighing cost
            and operational risk. You are conservative: intrusive actions (ROLLBACK, RESTART)
            and permanent spend (SCALE_UP) all carry cost and risk, so you endorse them only
            when the evidence clearly justifies it. When the impact is limited or the cause is
            still unclear, you prefer MONITOR — the cheapest safe move — over acting blindly.

            Vote for exactly ONE action: ROLLBACK, SCALE_UP, RESTART, or MONITOR.
            Give a confidence from 0 to 100 and a single-sentence rationale.
            """)
    @UserMessage("""
            Incident: {incidentInfo.system}/{incidentInfo.service} (priority P{incidentInfo.priority})
            Description: {incidentInfo.description}
            Operator report: {report}

            Cast your vote.
            """)
    @Agent(description = "Votes on incident remediation from a cost / operational-risk perspective.",
           outputKey = "costVote")
    Vote vote(IncidentInfo incidentInfo, String report);
}

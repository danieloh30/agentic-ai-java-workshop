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

    // TODO Ex11: strengthen this cost/operational-risk persona using the guide.
    @SystemMessage("""
            Assess operational cost and disruption. Return an action, confidence 0-100,
            and a one-line rationale. Use only the supplied evidence.
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

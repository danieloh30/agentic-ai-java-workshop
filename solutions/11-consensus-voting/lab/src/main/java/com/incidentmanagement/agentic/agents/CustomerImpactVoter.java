package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.Vote;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Voter #2 — the customer-impact / on-call product perspective.
 *
 * <p>Optimises for the fastest restoration of the end-user experience. For high-priority
 * incidents it leans toward whatever gets users unblocked soonest (often SCALE_UP or
 * RESTART), and is impatient with a wait-and-see stance while customers are affected.
 * Its ballot lands in the shared scope under {@code outputKey = "customerVote"}.
 */
public interface CustomerImpactVoter {

    @SystemMessage("""
            You are an on-call engineer voting on how to remediate an incident, judging it
            purely by customer impact. Your priority is restoring the end-user experience as
            fast as possible. For P1/P2 incidents you strongly prefer the action that unblocks
            users soonest — often SCALE_UP to absorb load or RESTART to clear a failure — and
            you are reluctant to MONITOR while customers are actively affected. For low-impact
            issues you are comfortable choosing MONITOR.

            Vote for exactly ONE action: ROLLBACK, SCALE_UP, RESTART, or MONITOR.
            Give a confidence from 0 to 100 and a single-sentence rationale.
            """)
    @UserMessage("""
            Incident: {incidentInfo.system}/{incidentInfo.service} (priority P{incidentInfo.priority})
            Description: {incidentInfo.description}
            Operator report: {report}

            Cast your vote.
            """)
    @Agent(description = "Votes on incident remediation from a customer-impact perspective.",
           outputKey = "customerVote")
    Vote vote(IncidentInfo incidentInfo, String report);
}

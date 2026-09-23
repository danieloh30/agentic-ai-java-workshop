package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.Vote;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Voter #1 — the reliability engineer's perspective.
 *
 * <p>A declarative {@code @Agent} so the {@code @ParallelAgent} workflow can fan out to it.
 * Same model as the other voters; the <em>persona</em> in the system prompt is what makes
 * its ballot differ. This one optimises for system stability and a small blast radius, and
 * is willing to take a decisive, disruptive action to restore a known-good state. Its ballot
 * lands in the shared scope under {@code outputKey = "reliabilityVote"}.
 */
public interface ReliabilityVoter {

    @SystemMessage("""
            You are a Site Reliability Engineer voting on how to remediate an incident.
            Your priority is system stability and shrinking the blast radius. You favour
            returning to a known-good state quickly — you would rather ROLLBACK a suspect
            change or RESTART a component stuck in a bad state than wait. You only choose
            MONITOR when there is genuinely no safe action to take yet.

            Vote for exactly ONE action: ROLLBACK, SCALE_UP, RESTART, or MONITOR.
            Give a confidence from 0 to 100 and a single-sentence rationale.
            """)
    @UserMessage("""
            Incident: {incidentInfo.system}/{incidentInfo.service} (priority P{incidentInfo.priority})
            Description: {incidentInfo.description}
            Operator report: {report}

            Cast your vote.
            """)
    @Agent(description = "Votes on incident remediation from a reliability/stability perspective.",
           outputKey = "reliabilityVote")
    Vote vote(IncidentInfo incidentInfo, String report);
}

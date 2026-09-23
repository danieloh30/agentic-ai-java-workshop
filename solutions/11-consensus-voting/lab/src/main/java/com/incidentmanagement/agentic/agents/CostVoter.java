package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.Vote;

/**
 * Voter #3 — the cost / operational-risk perspective. (Exercise 11, Step 1)
 *
 * <p>The other two voters ({@link ReliabilityVoter}, {@link CustomerImpactVoter}) are written
 * for you — read them as your template. Your job is to give this voter a distinct persona and
 * turn it into a declarative {@code @Agent} that returns a structured {@link Vote}.
 *
 * <p>TODO (Step 1):
 *   1. Add {@code @SystemMessage("""...""")} with a cost/operational-risk persona: conservative,
 *      prefers MONITOR or the cheapest safe move, only endorses ROLLBACK/RESTART/SCALE_UP when
 *      the evidence clearly justifies the cost or risk. Tell it to pick exactly ONE action
 *      (ROLLBACK, SCALE_UP, RESTART, MONITOR), a confidence 0–100, and a one-line rationale.
 *   2. Add {@code @UserMessage("""...""")} referencing {incidentInfo.system},
 *      {incidentInfo.service}, P{incidentInfo.priority}, {incidentInfo.description} and {report}.
 *   3. Add {@code @Agent(description = "...", outputKey = "costVote")} so the @ParallelAgent
 *      workflow can fan out to it and find its ballot in the shared scope.
 *
 * Imports you'll need:
 *   import dev.langchain4j.agentic.Agent;
 *   import dev.langchain4j.service.SystemMessage;
 *   import dev.langchain4j.service.UserMessage;
 */
public interface CostVoter {

    // TODO Step 1: annotate this method (@SystemMessage / @UserMessage / @Agent(outputKey="costVote"))
    Vote vote(IncidentInfo incidentInfo, String report);
}

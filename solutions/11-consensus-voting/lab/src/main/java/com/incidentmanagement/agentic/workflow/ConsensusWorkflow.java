package com.incidentmanagement.agentic.workflow;

import com.incidentmanagement.model.ConsensusResult;
import com.incidentmanagement.model.IncidentInfo;

import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * Consensus &amp; Voting workflow. (Exercise 11, Step 2)
 *
 * <p>Wire the three persona voters into a single parallel vote, then tally their ballots.
 *
 * <p>TODO (Step 2):
 *   1. Annotate {@code decide(...)} with
 *      {@code @ParallelAgent(outputKey = "consensusResult",
 *              subAgents = { ReliabilityVoter.class, CustomerImpactVoter.class, CostVoter.class })}
 *      so all three voters run in parallel over the same incident, each writing its Vote into
 *      the shared scope under its own outputKey (reliabilityVote / customerVote / costVote).
 *   2. Implement the {@code @Output tally(AgenticScope scope)} method below: read the three
 *      votes from the scope, count them per action, pick the majority (break ties by the higher
 *      summed confidence), and return a {@link ConsensusResult}.
 *
 * Reading a vote from the scope:
 *   Vote v = scope.readState("reliabilityVote", (Vote) null);
 *
 * Imports you'll need:
 *   import com.incidentmanagement.agentic.agents.ReliabilityVoter;
 *   import com.incidentmanagement.agentic.agents.CustomerImpactVoter;
 *   import com.incidentmanagement.agentic.agents.CostVoter;
 *   import com.incidentmanagement.model.RecommendedAction;
 *   import com.incidentmanagement.model.Vote;
 *   import dev.langchain4j.agentic.declarative.Output;
 *   import dev.langchain4j.agentic.declarative.ParallelAgent;
 *   (plus java.util.* for the tally)
 */
public interface ConsensusWorkflow {

    // TODO Step 2.1: add @ParallelAgent(outputKey = "consensusResult", subAgents = { ... })
    ConsensusResult decide(IncidentInfo incidentInfo, String report);

    // TODO Step 2.2: add @Output above this method and tally the three ballots from the scope.
    static ConsensusResult tally(AgenticScope scope) {
        throw new UnsupportedOperationException("TODO: tally the votes (Exercise 11)");
    }
}

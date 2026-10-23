package com.incidentmanagement.agentic.workflow;

import java.util.ArrayList;
import java.util.List;

import com.incidentmanagement.agentic.agents.CostVoter;
import com.incidentmanagement.agentic.agents.CustomerImpactVoter;
import com.incidentmanagement.agentic.agents.ReliabilityVoter;
import com.incidentmanagement.model.ConsensusResult;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.Vote;

import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.ParallelAgent;
import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * Consensus &amp; Voting, the agentic-native way.
 *
 * <p>{@code @ParallelAgent} fans out the <em>same</em> incident to three persona voters at
 * once — each an independent {@code @Agent} that writes its {@link Vote} into the shared
 * {@link AgenticScope} under its own {@code outputKey}. When all three have voted, the
 * {@code @Output} method tallies the ballots into a single {@link ConsensusResult}.
 *
 * <p>The "voting" is deterministic Java over structured records — not an LLM guessing a
 * winner — which is exactly why each voter returns a {@code Vote} record rather than prose.
 */
public interface ConsensusWorkflow {

    @ParallelAgent(
            outputKey = "consensusResult",
            subAgents = {
                    ReliabilityVoter.class,
                    CustomerImpactVoter.class,
                    CostVoter.class
            })
    ConsensusResult decide(IncidentInfo incidentInfo, String report);

    /**
     * Tally the three ballots. A strict majority wins; otherwise request operator review.
     */
    @Output
    static ConsensusResult tally(AgenticScope scope) {
        List<Vote> votes = new ArrayList<>();
        for (String key : List.of("reliabilityVote", "customerVote", "costVote")) {
            Vote v = scope.readState(key, (Vote) null);
            if (v != null) {
                votes.add(v);
            }
        }

        return ConsensusPolicy.tally(votes);
    }
}

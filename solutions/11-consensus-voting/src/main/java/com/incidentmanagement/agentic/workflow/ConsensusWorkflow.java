package com.incidentmanagement.agentic.workflow;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.incidentmanagement.agentic.agents.CostVoter;
import com.incidentmanagement.agentic.agents.CustomerImpactVoter;
import com.incidentmanagement.agentic.agents.ReliabilityVoter;
import com.incidentmanagement.model.ConsensusResult;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.RecommendedAction;
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
     * Tally the three ballots. Majority action wins; ties are broken by the higher summed
     * confidence of the tied actions.
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

        // Count votes and accumulate confidence per action.
        Map<RecommendedAction, Integer> counts = new EnumMap<>(RecommendedAction.class);
        Map<RecommendedAction, Integer> confidence = new EnumMap<>(RecommendedAction.class);
        for (Vote v : votes) {
            counts.merge(v.action(), 1, Integer::sum);
            confidence.merge(v.action(), v.confidence(), Integer::sum);
        }

        RecommendedAction winner = null;
        for (RecommendedAction action : counts.keySet()) {
            if (winner == null
                    || counts.get(action) > counts.get(winner)
                    || (counts.get(action).equals(counts.get(winner))
                        && confidence.get(action) > confidence.get(winner))) {
                winner = action;
            }
        }

        int agreement = winner == null ? 0 : counts.get(winner);
        boolean unanimous = !votes.isEmpty() && agreement == votes.size();
        return new ConsensusResult(winner, unanimous, agreement, votes.size(), votes);
    }
}

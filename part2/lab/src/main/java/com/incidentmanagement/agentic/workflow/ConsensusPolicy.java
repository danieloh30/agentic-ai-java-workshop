package com.incidentmanagement.agentic.workflow;

import com.incidentmanagement.model.*;
import java.util.*;

/** Deterministic decision boundary; confidence is not a substitute for agreement. */
public final class ConsensusPolicy {
    private ConsensusPolicy() {}
    public static ConsensusResult tally(List<Vote> ballots) {
        List<Vote> votes = ballots.stream().filter(Objects::nonNull).toList();
        boolean valid = votes.size() == 3 && votes.stream().allMatch(v -> v.action() != null
                && v.confidence() >= 0 && v.confidence() <= 100
                && v.rationale() != null && !v.rationale().isBlank());
        if (!valid) return new ConsensusResult(null, false, 0, 3, votes, true, "Missing or invalid ballot");
        Map<RecommendedAction, Integer> counts = new EnumMap<>(RecommendedAction.class);
        votes.forEach(v -> counts.merge(v.action(), 1, Integer::sum));
        var winner = counts.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow();
        int agreement = winner.getValue();
        // TODO Ex12: accept a strict majority (at least two of the three valid ballots).
        boolean accepted = agreement == 3;
        return new ConsensusResult(accepted ? winner.getKey() : null, agreement == 3, agreement, 3,
                votes, !accepted, accepted ? "Agreement threshold met; recommendation only" : "No majority; operator review required");
    }
}

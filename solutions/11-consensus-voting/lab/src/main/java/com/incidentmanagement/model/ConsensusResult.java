package com.incidentmanagement.model;

import java.util.List;

/**
 * The outcome of the vote: the winning {@link RecommendedAction}, whether the voters were
 * unanimous, how many backed the decision, and every individual ballot for transparency.
 */
public record ConsensusResult(
        RecommendedAction decision,
        boolean unanimous,
        int agreement,
        int totalVoters,
        List<Vote> votes) {
}

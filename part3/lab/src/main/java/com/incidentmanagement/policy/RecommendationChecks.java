package com.incidentmanagement.policy;

import com.incidentmanagement.model.Recommendation;

import java.util.List;

/** Deterministic contract checks; these cannot establish semantic correctness. */
public final class RecommendationChecks {
    private RecommendationChecks() {}

    public static List<String> violations(Recommendation r) {
        var errors = new java.util.ArrayList<String>();
        if (r == null) return List.of("Recommendation required");
        if (!List.of("INVESTIGATE", "ESCALATE").contains(r.action() == null ? "" : r.action()))
            errors.add("action must be INVESTIGATE or ESCALATE");
        if (!r.requiresApproval()) errors.add("requiresApproval must be true");
        if (r.rationale() == null || r.rationale().isBlank()) errors.add("rationale required");
        if (r.evidence() == null
                || r.evidence().isEmpty()
                || r.evidence().stream().anyMatch(s -> s == null || s.isBlank()))
            errors.add("nonempty evidence required");
        if (r.verificationChecks() == null
                || r.verificationChecks().isEmpty()
                || r.verificationChecks().stream().anyMatch(s -> s == null || s.isBlank()))
            errors.add("concrete verification checks required");
        return errors;
    }

    public static boolean grounded(Recommendation r, String report) {
        return r != null
                && r.evidence() != null
                && !r.evidence().isEmpty()
                && r.evidence().stream()
                        .allMatch(s -> s != null && !s.isBlank() && report.contains(s));
    }
}

package com.incidentmanagement.model;

/**
 * The reviewer's verdict after a round of execution. When {@link #resolved()} is false,
 * {@link #feedback()} tells the planner what is still missing so it can re-plan
 * (Plan &amp; Execute's dynamic re-planning step).
 */
public record ResolutionReview(boolean resolved, String feedback) {
}

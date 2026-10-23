package com.incidentmanagement.model;

/**
 * The reviewer's verdict on recommendation completeness, not production recovery. When {@link #recommendationReady()} is false,
 * {@link #feedback()} tells the planner what is still missing so it can re-plan
 * (Plan &amp; Execute's dynamic re-planning step).
 */
public record ResolutionReview(boolean recommendationReady, String feedback) {
}

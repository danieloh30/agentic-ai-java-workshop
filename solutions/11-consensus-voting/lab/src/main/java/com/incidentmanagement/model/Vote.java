package com.incidentmanagement.model;

/**
 * One voter's ballot. This is the structured output each persona agent must return —
 * a single {@link RecommendedAction}, a confidence 0–100, and a one-line rationale.
 *
 * <p>Because it's a record (not free text), the {@code ConsensusService} can count
 * votes and weigh confidence programmatically instead of parsing prose.
 */
public record Vote(RecommendedAction action, int confidence, String rationale) {
}

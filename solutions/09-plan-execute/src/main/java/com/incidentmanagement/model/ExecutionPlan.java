package com.incidentmanagement.model;

import java.util.List;

/**
 * The plan the {@code PlannerAgent} produces in the "Plan" phase: an ordered list of
 * specialist steps to run, plus a one-line rationale. The executor runs {@link #steps}
 * in order; a re-plan can append more steps if the reviewer is not satisfied.
 */
public record ExecutionPlan(List<ResolutionStep> steps, String rationale) {
}

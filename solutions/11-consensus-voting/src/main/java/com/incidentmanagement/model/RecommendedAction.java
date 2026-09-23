package com.incidentmanagement.model;

/**
 * The set of remediation actions a voter can recommend for an incident.
 * Keeping the choices to a small closed enum is what makes the votes
 * <em>tallyable</em> — every voter must pick exactly one of these.
 */
public enum RecommendedAction {
    /** Revert the most recent change/deploy. */
    ROLLBACK,
    /** Add capacity (replicas, memory, connections). */
    SCALE_UP,
    /** Restart the failing component to clear a bad state. */
    RESTART,
    /** Hold and keep watching; not yet worth an intrusive action. */
    MONITOR
}

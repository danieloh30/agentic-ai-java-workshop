package com.incidentmanagement.model;

/**
 * The specialist steps the planner can schedule. The planner chooses an ordered
 * subset of these — a low-priority incident may skip {@link #COMMUNICATE}, while a
 * P1 will almost always include it.
 */
public enum ResolutionStep {
    /** Investigate and identify the most likely root cause. */
    DIAGNOSE,
    /** Take a concrete action to reduce or remove impact. */
    MITIGATE,
    /** Confirm whether the incident now appears resolved. */
    VERIFY,
    /** Write a stakeholder status update. */
    COMMUNICATE
}

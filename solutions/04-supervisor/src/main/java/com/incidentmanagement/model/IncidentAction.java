package com.incidentmanagement.model;

/**
 * Enum representing the type of possible incident assignments for incident processing
 */
public enum IncidentAction {
    ESCALATE,       // Incident needs to be escalated to management
    INVESTIGATE,    // Incident needs further investigation
    TRIAGE,
    MONITOR,         // Incident needs initial triage
    RESOLVE         // Incident can be resolved
}

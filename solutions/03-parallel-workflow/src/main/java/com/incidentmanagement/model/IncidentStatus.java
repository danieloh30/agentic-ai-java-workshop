package com.incidentmanagement.model;

/**
 * Enum representing the possible statuses of an incident.
 */
public enum IncidentStatus {
    OPEN("open"),
    TRIAGING("triaging"),
    IN_PROGRESS("in progress"),
    ESCALATED("escalated"),
    RESOLVED("resolved");

    private final String value;

    IncidentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}

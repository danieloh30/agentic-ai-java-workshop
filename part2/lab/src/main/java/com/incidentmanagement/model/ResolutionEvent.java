package com.incidentmanagement.model;

/** Processing completion is a recommendation, never proof of incident recovery. */
public record ResolutionEvent(String requestId, Integer incidentId, String processingStatus,
                              String triage, String resolution) {
}

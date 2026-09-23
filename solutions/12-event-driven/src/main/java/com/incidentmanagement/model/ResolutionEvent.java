package com.incidentmanagement.model;

/**
 * The result the agentic pipeline publishes onto the {@code resolutions-out} Kafka topic
 * after processing an {@link IncidentEvent}: the triage classification and the recommended
 * resolution, tied back to the originating incident id.
 */
public record ResolutionEvent(
        Integer incidentId,
        String triage,
        String resolution) {
}

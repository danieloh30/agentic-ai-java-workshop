package com.incidentmanagement.model;

/**
 * An incident published onto the {@code incidents-in} Kafka topic. It carries everything the
 * agentic pipeline needs, so the consumer can process the event without a database round-trip.
 *
 * <p>Serialized to/from JSON by Quarkus's automatic Jackson (de)serializer detection.
 */
public record IncidentEvent(
        Integer incidentId,
        String system,
        String service,
        String priority,
        String description,
        String report) {
}

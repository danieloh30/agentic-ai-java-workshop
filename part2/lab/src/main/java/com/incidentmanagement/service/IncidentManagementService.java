package com.incidentmanagement.service;

import com.incidentmanagement.agentic.agents.IncidentAssistant;
import com.incidentmanagement.messaging.IncidentPublisher;
import com.incidentmanagement.model.IncidentInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

/** Dashboard dispatch only; agent behavior remains in declarative interfaces. */
@ApplicationScoped
public class IncidentManagementService {
    @Inject PlanIncidentService planning;
    @Inject VotingIncidentService voting;
    @Inject IncidentAssistant assistant;
    @Inject IncidentPublisher publisher;

    public String processIncident(Integer id, String report, String mode) {
        if (report == null || report.isBlank()) throw new BadRequestException("Enter an operator report or message");
        if (IncidentInfo.findById(id) == null) throw new NotFoundException("Incident not found");
        return switch (mode) {
            case "PLAN" -> planning.processIncident(id, report);
            case "MEMORY" -> assistant.chat(id, report);
            case "VOTING" -> voting.processIncident(id, report);
            case "EVENTS" -> {
                var event = publisher.publish(id, report);
                yield "Submitted request " + event.requestId() + ". Processing continues; reopen to check the outcome.";
            }
            default -> throw new BadRequestException("Choose PLAN, MEMORY, VOTING, or EVENTS");
        };
    }
}

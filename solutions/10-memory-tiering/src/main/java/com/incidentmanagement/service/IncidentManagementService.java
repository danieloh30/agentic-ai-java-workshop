package com.incidentmanagement.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.incidentmanagement.agentic.agents.IncidentAssistant;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.IncidentStatus;
import io.quarkus.logging.Log;

/**
 * Opens a conversation with the {@link IncidentAssistant} for an incident. Wired to the
 * dashboard's "Process Incident" button ({@code POST /incident-management/process/{id}}).
 *
 * <p>This is turn 1 of the conversation: it seeds the assistant's memory with the incident
 * facts. Follow-up turns (via {@code POST /incident-assistant/{id}}) build on it — that's
 * the point of the two-tier memory.
 */
@ApplicationScoped
public class IncidentManagementService {

    @Inject
    IncidentAssistant incidentAssistant;

    @Transactional
    public String processIncident(Integer incidentNumber, String report) {
        IncidentInfo incident = IncidentInfo.findById(incidentNumber);
        if (incident == null) {
            return "Incident not found with number: " + incidentNumber;
        }

        String opening = """
                Here is the incident I need help with. Give me a brief initial assessment.

                System: %s
                Service: %s
                Priority: P%s
                Description: %s
                Operator report: %s
                """.formatted(incident.system, incident.service, incident.priority,
                incident.description, report == null ? "" : report);

        String reply = incidentAssistant.chat(incidentNumber, opening);
        Log.infof("Opened assistant conversation for incident #%d", incidentNumber);

        incident.status = IncidentStatus.IN_PROGRESS;
        incident.persist();

        return reply;
    }
}

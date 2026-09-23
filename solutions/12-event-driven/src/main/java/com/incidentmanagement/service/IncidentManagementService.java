package com.incidentmanagement.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import com.incidentmanagement.model.IncidentEvent;
import com.incidentmanagement.model.IncidentInfo;

import io.quarkus.logging.Log;

/**
 * Publishes an incident onto Kafka for the event-driven pipeline. Wired to the dashboard's
 * "Process Incident" button ({@code POST /incident-management/process/{id}}).
 *
 * <p>Unlike earlier exercises, this returns immediately: the triage/resolution work happens
 * asynchronously on the {@code incidents-in} consumer. Read the outcome from
 * {@code GET /incident-events/resolutions/{id}} once the pipeline finishes.
 */
@ApplicationScoped
public class IncidentManagementService {

    @Channel("incidents-out")
    Emitter<IncidentEvent> emitter;

    @Transactional
    public String processIncident(Integer incidentNumber, String report) {
        IncidentInfo incident = IncidentInfo.findById(incidentNumber);
        if (incident == null) {
            return "Incident not found with number: " + incidentNumber;
        }

        IncidentEvent event = new IncidentEvent(incidentNumber, incident.system, incident.service,
                incident.priority, incident.description, report == null ? "" : report);
        emitter.send(event);
        Log.infof("Published incident #%d to incidents-in (from dashboard)", incidentNumber);

        return "Incident #" + incidentNumber + " published to the pipeline. "
                + "The resolution will appear shortly at /incident-events/resolutions/" + incidentNumber + ".";
    }
}

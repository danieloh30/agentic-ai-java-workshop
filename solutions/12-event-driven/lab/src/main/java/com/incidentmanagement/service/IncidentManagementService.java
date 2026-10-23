package com.incidentmanagement.service;

import com.incidentmanagement.messaging.IncidentPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class IncidentManagementService {
    @Inject IncidentPublisher publisher;
    public String processIncident(Integer incidentNumber, String report) {
        var event = publisher.publish(incidentNumber, report);
        return "Incident #" + incidentNumber + " submitted. Request: " + event.requestId()
                + ". Processing completion produces a recommendation for operator review, not verified recovery.";
    }
}

package com.incidentmanagement.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.incidentmanagement.agentic.agents.TriageAgent;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.IncidentStatus;

@ApplicationScoped
public class IncidentManagementService {

    // --8<-- [start:processIncident]
    @Inject
    TriageAgent triageAgent;

    @Transactional
    public String processIncident(Integer incidentNumber, String report) {
        IncidentInfo incidentInfo = IncidentInfo.findById(incidentNumber);
        if (incidentInfo == null) {
            return "Incident not found with number: " + incidentNumber;
        }

        String result = triageAgent.processTriage(incidentInfo, incidentNumber, report);

        if (result.toUpperCase().contains("TRIAGE_NOT_REQUIRED")) {
            incidentInfo.status = IncidentStatus.RESOLVED;
            incidentInfo.persist();
        }

        return result;
    }
    // --8<-- [end:processIncident]
}

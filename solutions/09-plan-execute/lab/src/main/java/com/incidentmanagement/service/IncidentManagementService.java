package com.incidentmanagement.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.incidentmanagement.agentic.agents.IncidentPlannerAgent;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.IncidentStatus;
import io.quarkus.logging.Log;

/**
 * Drives an incident through the declarative {@code @PlannerAgent} and persists the
 * outcome. Wired to the dashboard's "Process Incident" button
 * ({@code POST /incident-management/process/{id}}).
 */
@ApplicationScoped
public class IncidentManagementService {

    @Inject
    IncidentPlannerAgent incidentPlannerAgent;

    @Transactional
    public String processIncident(Integer incidentNumber, String report) {
        IncidentInfo incidentInfo = IncidentInfo.findById(incidentNumber);
        if (incidentInfo == null) {
            return "Incident not found with number: " + incidentNumber;
        }

        String result = incidentPlannerAgent.resolveIncident(incidentInfo, incidentNumber, report);
        Log.infof("Plan & Execute finished for incident #%d", incidentNumber);

        incidentInfo.status = IncidentStatus.RESOLVED;
        incidentInfo.description = result;
        incidentInfo.persist();

        return result;
    }
}

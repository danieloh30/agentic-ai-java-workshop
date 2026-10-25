package com.incidentmanagement.agentic.workflow;

import com.incidentmanagement.model.IncidentInfo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.UUID;

/** REST-to-workflow input mapping. AI behavior and orchestration live in interfaces. */
@ApplicationScoped
public class IncidentReportFlow {
    @Inject IncidentReportWorkflow workflow;

    public Map<String, Object> generateReport(IncidentInfo incident) {
        return workflow.generateReport(
                UUID.randomUUID().toString(),
                incident.system,
                incident.service,
                incident.priority,
                incident.description == null ? "" : incident.description,
                incident.status.toString());
    }
}

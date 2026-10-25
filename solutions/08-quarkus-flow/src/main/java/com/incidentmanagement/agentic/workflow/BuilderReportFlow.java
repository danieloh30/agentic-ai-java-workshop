package com.incidentmanagement.agentic.workflow;

import com.incidentmanagement.agentic.agents.ReportCriticAgent;
import com.incidentmanagement.agentic.agents.ReportDrafterAgent;
import com.incidentmanagement.model.IncidentInfo;

import dev.langchain4j.agentic.AgenticServices;

import io.quarkus.arc.ClientProxy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.UUID;

/** Optional comparison only. The dashboard uses the declarative IncidentReportWorkflow. */
@ApplicationScoped
public class BuilderReportFlow {
    @Inject ReportDrafterAgent drafter;
    @Inject ReportCriticAgent critic;

    @SuppressWarnings("unchecked")
    public Map<String, Object> generateReport(IncidentInfo incident) {
        var workflow =
                AgenticServices.loopBuilder()
                        .name("incident-report-builder-comparison")
                        .subAgents(ClientProxy.unwrap(drafter), ClientProxy.unwrap(critic))
                        .beforeCall(IncidentReportWorkflow::initialize)
                        .maxIterations(3)
                        .testExitAtLoopEnd(true)
                        .exitCondition(IncidentReportWorkflow::qualitySatisfied)
                        .output(IncidentReportWorkflow::result)
                        .build();
        return (Map<String, Object>)
                workflow.invoke(
                        Map.of(
                                "reportInvocationId",
                                UUID.randomUUID().toString(),
                                "system",
                                incident.system,
                                "service",
                                incident.service,
                                "priority",
                                incident.priority,
                                "description",
                                incident.description == null ? "" : incident.description,
                                "status",
                                incident.status.toString()));
    }
}

package com.incidentmanagement.agentic.workflow;

import com.incidentmanagement.agentic.agents.ResolutionAgent;
import com.incidentmanagement.model.IncidentOutcome;
import com.incidentmanagement.model.IncidentAction;
import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.MonitoredAgent;

/**
 * Workflow for processing incidents using a sequence of agents.
 */
public interface IncidentProcessingWorkflow extends MonitoredAgent {

    /**
     * Processes an incident by running report analysis, assignment, and then resolution.
     */
    // --8<-- [start:sequence-agent]
    @SequenceAgent(outputKey = "incidentProcessingAgentResult",
            subAgents = { ReportAnalysisWorkflow.class, IncidentAssignmentWorkflow.class, ResolutionAgent.class })
    // --8<-- [end:sequence-agent]
    IncidentOutcome processIncident(
            IncidentInfo incidentInfo,
            Integer incidentNumber,
            String report);

    @Output
    static IncidentOutcome output(String incidentResolution, String diagnosticRequest, String triageRequest) {
        IncidentAction incidentAction;
        // Check diagnostic first (higher priority)
        if (isRequired(diagnosticRequest)) {
            incidentAction = IncidentAction.INVESTIGATE;
        } else if (isRequired(triageRequest)) {
            incidentAction = IncidentAction.TRIAGE;
        } else {
            incidentAction = IncidentAction.MONITOR;
        }
        return new IncidentOutcome(incidentResolution, incidentAction);
    }

    private static boolean isRequired(String value) {
        return value != null && !value.isEmpty() && !value.toUpperCase().contains("NOT_REQUIRED");
    }
}

package com.incidentmanagement.agentic.workflow;

import com.incidentmanagement.agentic.agents.DiagnosticAgent;
import com.incidentmanagement.agentic.agents.TriageAgent;
import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.declarative.ActivationCondition;
import dev.langchain4j.agentic.declarative.ConditionalAgent;

/**
 * Workflow for assigning incidents to appropriate teams based on report analysis.
 */
public interface IncidentAssignmentWorkflow {

    /**
     * Assigns the incident to the appropriate team based on the report analysis.
     */
    // --8<-- [start:conditional-agent]
    @ConditionalAgent(outputKey = "analysisResult",
            subAgents = { DiagnosticAgent.class, TriageAgent.class })
    String processAction(
            IncidentInfo incidentInfo,
            Integer incidentNumber,
            String triageRequest,
            String diagnosticRequest);

    @ActivationCondition(DiagnosticAgent.class)
    static boolean assignToDiagnostic(String diagnosticRequest) {
        return isRequired(diagnosticRequest);
    }

    @ActivationCondition(TriageAgent.class)
    static boolean assignToTriage(String diagnosticRequest, String triageRequest) {
        return !isRequired(diagnosticRequest) && isRequired(triageRequest);
    }

    private static boolean isRequired(String value) {
        return value != null && !value.isEmpty() && !value.toUpperCase().contains("NOT_REQUIRED");
    }
    // --8<-- [end:conditional-agent]
}

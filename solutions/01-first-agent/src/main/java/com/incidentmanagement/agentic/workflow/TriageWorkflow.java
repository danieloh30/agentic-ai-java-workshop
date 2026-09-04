package com.incidentmanagement.agentic.workflow;

import com.incidentmanagement.agentic.agents.TriageAgent;
import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;

/**
 * Agentic system entry point for incident triage.
 */
public interface TriageWorkflow {

    @SequenceAgent(outputKey = "triageResult", subAgents = { TriageAgent.class })
    String processTriage(
            IncidentInfo incidentInfo,
            Integer incidentNumber,
            String report);

    @Output
    static String output(String analysisResult) {
        return analysisResult;
    }
}

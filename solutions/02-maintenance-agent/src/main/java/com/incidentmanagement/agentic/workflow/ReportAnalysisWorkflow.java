package com.incidentmanagement.agentic.workflow;

import com.incidentmanagement.agentic.agents.TriageFeedbackAgent;
import com.incidentmanagement.agentic.agents.DiagnosticFeedbackAgent;
import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.declarative.ParallelAgent;

/**
 * Workflow for processing incident reports in parallel.
 */
public interface ReportAnalysisWorkflow {

    /**
     * Runs multiple analysis agents in parallel to analyze different aspects of an incident report.
     */
    // --8<-- [start:parallel-agent]
    @ParallelAgent(outputKey = "reportAnalysisResult",
            subAgents = { TriageFeedbackAgent.class, DiagnosticFeedbackAgent.class })
    // --8<-- [end:parallel-agent]
    String analyzeReport(
            IncidentInfo incidentInfo,
            Integer incidentNumber,
            String report);
}

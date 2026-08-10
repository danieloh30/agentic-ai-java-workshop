package com.incidentmanagement.agentic.workflow;

import com.incidentmanagement.agentic.agents.IncidentAnalysisAgent;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.IncidentAnalysisResults;
import com.incidentmanagement.model.AnalysisTask;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.ParallelMapperAgent;
import dev.langchain4j.agentic.scope.AgenticScope;

import java.util.List;

/**
 * Workflow for processing incident reports in parallel.
 * Analyzes incidents for severity, impact, and resolution needs using a unified agent.
 */
public interface IncidentAnalysisWorkflow {

    /**
     * Runs the incident analysis agent in parallel for multiple tasks.
     * Uses @ParallelMapperAgent to execute the same agent with different task configurations.
     * Returns a list of results that will be mapped to individual output keys.
     */
    // --8<-- [start:parallel-mapper-agent]
    @ParallelMapperAgent(
            description = "Analyzes incident reports in parallel for severity, impact, and resolution needs",
            outputKey = "incidentAnalysisResults",
            subAgent = IncidentAnalysisAgent.class,
            itemsProvider = "tasks")
    // --8<-- [end:parallel-mapper-agent]
    IncidentAnalysisResults analyzeIncident(
            List<AnalysisTask> tasks,
            IncidentInfo incidentInfo,
            Integer incidentNumber,
            String report);

    /**
     * Output method that transforms the parallel analysis results into a structured object.
     * The incidentAnalysisResults list contains results in the same order as the input tasks:
     * [0] = severity analysis, [1] = impact analysis, [2] = resolution analysis
     */
    @Output
    static IncidentAnalysisResults output(AgenticScope scope, List<String> incidentAnalysisResults) {
        return new IncidentAnalysisResults(
                incidentAnalysisResults.get(0),  // severityAnalysis
                incidentAnalysisResults.get(1),  // impactAnalysis
                incidentAnalysisResults.get(2)   // resolutionAnalysis
        );
    }
}

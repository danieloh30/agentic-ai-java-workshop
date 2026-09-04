package com.incidentmanagement.agentic.workflow;

import com.incidentmanagement.agentic.agents.TriageAgent;
import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;

/**
 * Agentic system entry point for incident triage.
 */
public interface TriageWorkflow {

    // TODO Exercise 1 — Step 1b: Add @SequenceAgent annotation — See docs/01-first-agent/START_HERE.md
    String processTriage(
            IncidentInfo incidentInfo,
            Integer incidentNumber,
            String report);

    // TODO Exercise 1 — Step 1b: Add @Output annotation — See docs/01-first-agent/START_HERE.md
    static String output(String analysisResult) {
        return analysisResult;
    }
}

package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.AnalysisTask;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface IncidentAnalysisAgent {

    // TODO Exercise 3 — Step 1: Add @SystemMessage, @UserMessage, and @Agent annotations — See docs/03-parallel-workflow/START_HERE.md
    String analyzeIncident(AnalysisTask task, IncidentInfo incidentInfo,
                           Integer incidentNumber, String report);

}

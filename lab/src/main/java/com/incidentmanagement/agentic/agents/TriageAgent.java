package com.incidentmanagement.agentic.agents;

import io.quarkiverse.langchain4j.ToolBox;

import com.incidentmanagement.agentic.tools.TriageTool;
import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface TriageAgent {

    // TODO Exercise 1 — Step 1: Add @SystemMessage, @UserMessage, @Agent, and @ToolBox annotations — See docs/01-first-agent/START_HERE.md
    String processTriage(IncidentInfo incidentInfo, Integer incidentNumber, String report);

}

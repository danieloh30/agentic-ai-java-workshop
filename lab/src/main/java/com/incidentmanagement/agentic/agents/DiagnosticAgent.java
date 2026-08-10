package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface DiagnosticAgent {

    // TODO Exercise 2 — Step 1: Add @SystemMessage, @UserMessage, and @Agent annotations — See docs/02-maintenance-agent/START_HERE.md
    String processDiagnostic(IncidentInfo incidentInfo, Integer incidentNumber, String diagnosticRequest);

}

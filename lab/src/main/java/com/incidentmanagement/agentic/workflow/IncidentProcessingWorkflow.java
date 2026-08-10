package com.incidentmanagement.agentic.workflow;

import com.incidentmanagement.agentic.agents.ResolutionAgent;
import com.incidentmanagement.agentic.agents.IncidentSupervisorAgent;
import com.incidentmanagement.model.IncidentOutcome;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.AnalysisTask;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import io.quarkus.logging.Log;

import java.util.List;

// TODO Exercise 4 — Step 5: Add "extends MonitoredAgent" — See docs/04-supervisor/START_HERE.md
public interface IncidentProcessingWorkflow {

    // TODO Exercise 4 — Step 5a: Add @SequenceAgent annotation — See docs/04-supervisor/START_HERE.md
    IncidentOutcome processIncident(List<AnalysisTask> tasks, IncidentInfo incidentInfo,
                                     Integer incidentNumber, String report);

    // TODO Exercise 4 — Step 5b: @Output static method — See docs/04-supervisor/START_HERE.md

}

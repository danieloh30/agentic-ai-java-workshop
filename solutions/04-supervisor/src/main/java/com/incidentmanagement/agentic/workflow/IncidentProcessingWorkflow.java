package com.incidentmanagement.agentic.workflow;

import com.incidentmanagement.agentic.agents.ResolutionAgent;
import com.incidentmanagement.agentic.agents.IncidentSupervisorAgent;
import com.incidentmanagement.model.IncidentOutcome;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.AnalysisTask;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.MonitoredAgent;
import io.quarkus.logging.Log;

import java.util.List;

public interface IncidentProcessingWorkflow extends MonitoredAgent {

    @SequenceAgent(outputKey = "incidentProcessingAgentResult",
            subAgents = { IncidentAnalysisWorkflow.class,
                          IncidentSupervisorAgent.class,
                          ResolutionAgent.class })
    IncidentOutcome processIncident(List<AnalysisTask> tasks, IncidentInfo incidentInfo,
                                     Integer incidentNumber, String report);

    @Output
    static IncidentOutcome output(IncidentOutcome incidentOutcome) {
        Log.debug("IncidentOutcome: " + incidentOutcome.resolution()
                  + " → " + incidentOutcome.incidentAction());
        return incidentOutcome;
    }
}

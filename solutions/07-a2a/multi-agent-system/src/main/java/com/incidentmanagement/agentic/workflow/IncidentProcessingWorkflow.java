package com.incidentmanagement.agentic.workflow;

import com.incidentmanagement.agentic.agents.EscalationProposalAgent;
import com.incidentmanagement.agentic.agents.HumanApprovalAgent;
import com.incidentmanagement.agentic.agents.ResolutionAgent;
import com.incidentmanagement.agentic.agents.IncidentSupervisorAgent;
import com.incidentmanagement.model.IncidentOutcome;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.AnalysisTask;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.MonitoredAgent;
import dev.langchain4j.data.message.ImageContent;
import io.quarkus.logging.Log;

import java.util.List;

public interface IncidentProcessingWorkflow extends MonitoredAgent {

    @SequenceAgent(outputKey = "incidentProcessingAgentResult",
            subAgents = {
                          // TODO (Exercise 7 — multimodal task): add IncidentLogAnalysisAgent.class
                          //   here as the FIRST sub-agent. It reads the uploaded log screenshot and
                          //   rewrites "report" (outputKey = "report") before the parallel analysis
                          //   consumes it. Without this line the "Choose File" upload has no effect.
                          //   See docs/07-a2a/START_HERE.md → "Multimodal log analysis".
                          IncidentAnalysisWorkflow.class,
                          IncidentSupervisorAgent.class,
                          EscalationProposalAgent.class,
                          HumanApprovalAgent.class,
                          ResolutionAgent.class })
    IncidentOutcome processIncident(List<AnalysisTask> tasks, IncidentInfo incidentInfo,
                                     Integer incidentNumber, String report,
                                     ImageContent logImage);

    @Output
    static IncidentOutcome output(IncidentOutcome incidentOutcome) {
        Log.debug("IncidentOutcome: " + incidentOutcome.resolution()
                  + " → " + incidentOutcome.incidentAction());
        return incidentOutcome;
    }
}

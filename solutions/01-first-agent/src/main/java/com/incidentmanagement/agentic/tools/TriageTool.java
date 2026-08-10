package com.incidentmanagement.agentic.tools;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.IncidentStatus;
import dev.langchain4j.agent.tool.Tool;
import io.quarkus.logging.Log;

@ApplicationScoped
public class TriageTool {

    @Tool("Requests initial triage with the specified options")
    @Transactional
    public String requestTriage(
            Integer incidentNumber,
            String system,
            String service,
            Integer priority,
            boolean assignOnCall,
            boolean notifyStakeholders,
            boolean createWarRoom,
            boolean linkRelatedIncidents,
            String triageNotes) {

        IncidentInfo incidentInfo = IncidentInfo.findById(incidentNumber);
        if (incidentInfo != null) {
            incidentInfo.status = IncidentStatus.TRIAGING;
            incidentInfo.persist();
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Triage requested for ").append(system).append("/")
               .append(service).append(" (P").append(priority).append("), Incident #")
               .append(incidentNumber).append(":\n");
        if (assignOnCall)        summary.append("- Assign on-call engineer\n");
        if (notifyStakeholders)  summary.append("- Notify stakeholders\n");
        if (createWarRoom)       summary.append("- Create war room\n");
        if (linkRelatedIncidents) summary.append("- Link related incidents\n");
        if (triageNotes != null && !triageNotes.isEmpty())
            summary.append("Notes: ").append(triageNotes);

        Log.info("  └─ TriageTool activated for incident #" + incidentNumber);
        return summary.toString();
    }
}

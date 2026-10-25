package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.ReportCritique;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ReportCriticAgent {

    // TODO Exercise 08 — Step 1b
    // Replace this TODO block and placeholder method with the full agent declaration
    // from docs/08-quarkus-flow/START_HERE.md. Keep the supplied imports.

    @UserMessage(
            "Evaluate this post-incident report for {system}/{service} ({priority}), described as"
                + " {description} with status {status}: {report}")
    @Agent(
            description =
                    "Reviews an incident report for accuracy, completeness, and actionable"
                        + " recommendations.",
            outputKey = "critique")
    ReportCritique critiqueReport(
            @MemoryId String reportInvocationId,
            String system,
            String service,
            String priority,
            String description,
            String status,
            String report);
}

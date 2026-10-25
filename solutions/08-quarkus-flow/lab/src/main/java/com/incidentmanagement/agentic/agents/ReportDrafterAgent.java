package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.ReportCritique;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ReportDrafterAgent {

    // TODO Exercise 08 — Step 1a
    // Replace this TODO block and placeholder method with the full agent declaration
    // from docs/08-quarkus-flow/START_HERE.md. Keep the supplied imports.

    @UserMessage(
            "Draft a post-incident report for {system}/{service} ({priority}): {description} —"
                + " status: {status}. Feedback: {critique}")
    @Agent(
            description =
                    "Drafts an incident report using authoritative facts and the previous"
                        + " critique.",
            outputKey = "report")
    String draftReport(
            @MemoryId String reportInvocationId,
            String system,
            String service,
            String priority,
            String description,
            String status,
            ReportCritique critique);
}

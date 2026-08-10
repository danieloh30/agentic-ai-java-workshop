package com.incidentmanagement.agentic.agents;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RegisterAiService
public interface ReportDrafterAgent {

    // TODO Exercise 08 — Step 1a
    // Replace the placeholder @UserMessage below with the @SystemMessage and @UserMessage
    // annotations from docs/08-quarkus-flow/START_HERE.md.

    @UserMessage("Draft a post-incident report for {system}/{service} ({priority}): {description} — status: {status}. Feedback: {feedback}")
    String draftReport(String system, String service,
                       String priority, String description,
                       String status, String feedback);
}

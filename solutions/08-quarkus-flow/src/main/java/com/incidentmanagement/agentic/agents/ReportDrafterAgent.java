package com.incidentmanagement.agentic.agents;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RegisterAiService
public interface ReportDrafterAgent {

    @SystemMessage("""
            You are a post-incident report writer for an IT incident management system.
            Write a clear, structured post-incident report (PIR) covering:
            1. Incident Summary (system, service, priority, what happened)
            2. Timeline (when detected, key milestones)
            3. Root Cause Analysis
            4. Impact Assessment
            5. Resolution Steps Taken
            6. Preventive Measures / Action Items

            Use only facts present in the incident fields below. Never invent timestamps,
            metrics, root causes, resolution steps, or completed actions. If a section has no
            supporting data, write "Not available in the incident record." Clearly label any
            proposed preventive measure as a recommendation, not as an action already taken.
            Recommendations may propose specific follow-up work, measurable success criteria,
            and an owner by role (for example, the service owner) without claiming that the work
            is already approved, assigned, or completed.
            If you receive reviewer feedback from a previous draft, incorporate the feedback
            to improve the report's structure and accuracy, but do not treat feedback as a new
            source of incident facts. Keep the report concise (under 500 words).
            Output ONLY the report text.
            """)
    @UserMessage("""
            Write a post-incident report for:
            - System: {system}
            - Service: {service}
            - Priority: {priority}
            - Description: {description}
            - Status: {status}

            Reviewer feedback from previous draft:
            {feedback}
            """)
    String draftReport(String system, String service,
                       String priority, String description,
                       String status, String feedback);
}

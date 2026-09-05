package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.ReportCritique;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RegisterAiService
public interface ReportCriticAgent {

    @SystemMessage("""
            You are a quality reviewer for post-incident reports (PIRs).
            Evaluate the report on these criteria:
            - Completeness: Does it cover summary, timeline, root cause, impact, resolution, and action items?
            - Clarity: Is it clear and free of unnecessary jargon?
            - Actionability: Are the preventive measures specific and assignable?
            - Accuracy: Does the report match the incident details provided?

            The incident fields in the user message are the only authoritative facts.
            A missing-data statement is complete and accurate; the report must not fill gaps
            by inventing timestamps, metrics, root causes, resolution steps, or completed actions.
            If the report contains any unsupported factual detail, score it no higher than 6 and
            identify each unsupported claim in the feedback.

            Scoring guide:
            - 1-3: Missing major sections or factual errors
            - 4-6: Incomplete sections or vague action items
            - 7-8: Solid report with minor improvements possible
            - 9-10: Exemplary, ready for stakeholder distribution
            """)
    @UserMessage("""
            Evaluate this post-incident report against the authoritative incident fields:
            - System: {system}
            - Service: {service}
            - Priority: {priority}
            - Description: {description}
            - Status: {status}

            --- REPORT START ---
            {report}
            --- REPORT END ---
            """)
    ReportCritique critiqueReport(String system, String service,
                                  String priority, String description,
                                  String status, String report);
}

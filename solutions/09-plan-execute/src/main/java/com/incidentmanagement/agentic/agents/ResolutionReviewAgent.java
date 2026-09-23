package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.ResolutionReview;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The gate between "Execute" and a possible re-plan. After a round of specialists has run,
 * this reviewer judges whether the incident is actually resolved. If not, its feedback is
 * fed back to the {@link PlannerAgent} so it can plan the remaining steps.
 */
@ApplicationScoped
@RegisterAiService
public interface ResolutionReviewAgent {

    @SystemMessage("""
            You are a resolution reviewer for an IT incident-management system.
            Given the incident and the specialists' outputs so far, decide whether the
            incident can be considered resolved.

            Mark it resolved when there is a diagnosis, a mitigation, and a verification whose
            verdict is RESOLVED. If any of those is missing, or the verification verdict is
            NOT RESOLVED, then it is not resolved — say concisely what step is still needed so
            the planner can schedule it. Trust the verification verdict; do not demand
            production monitoring evidence beyond it. Judge only from the outputs provided.
            """)
    @UserMessage("""
            Incident: {system}/{service} (priority {priority}, #{incidentNumber})

            Diagnosis: {diagnosis}
            Mitigation: {mitigation}
            Verification: {verification}
            Communication: {communication}
            """)
    ResolutionReview review(String system, String service, String priority, Integer incidentNumber,
                            String diagnosis, String mitigation, String verification, String communication);
}

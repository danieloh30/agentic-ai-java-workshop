package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.ResolutionReview;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.agentic.Agent;

/**
 * The gate between "Execute" and a possible re-plan. After a round of specialists has run,
 * this reviewer judges whether the recommendation is complete. If not, its feedback is
 * fed back to the {@link PlannerAgent} so it can plan the remaining steps.
 */
public interface ResolutionReviewAgent {

    @SystemMessage("""
            You review the completeness of an IT incident response recommendation.
            Set recommendationReady=true only when diagnosis, proposed mitigation, and
            a READY verification assessment are present; require a stakeholder update for P1/P2.
            If work is missing or verification says NEEDS_REVISION, return false and concise
            feedback naming the additional work needed. Do not claim production recovery:
            the specialists only recommend actions and checks, and never execute them.
            """)
    @UserMessage("""
            Incident: {system}/{service} (priority {priority}, #{incidentNumber})

            Diagnosis: {diagnosis}
            Mitigation: {mitigation}
            Verification: {verification}
            Communication: {communication}
            """)
    @Agent(description = "Reviews recommendation completeness and identifies additional work.",
           outputKey = "review")
    ResolutionReview review(String system, String service, String priority, Integer incidentNumber,
                            String diagnosis, String mitigation, String verification, String communication);
}

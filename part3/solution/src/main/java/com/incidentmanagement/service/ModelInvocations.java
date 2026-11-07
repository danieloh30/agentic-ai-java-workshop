package com.incidentmanagement.service;

import com.incidentmanagement.agents.*;
import com.incidentmanagement.model.Recommendation;
import com.incidentmanagement.policy.RecommendationChecks;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/** Invocation lifecycle and bounded preview repair. Prompts remain in declarative agent interfaces. */
@ApplicationScoped
public class ModelInvocations {
    @Inject ResponseWorkflow response;
    @Inject QualityWorkflow quality;

    public Recommendation recommend(String priority, String report) {
        return response.recommend(UUID.randomUUID().toString(), priority, report, false);
    }

    /** One extra workflow invocation only when preview evidence fails the exact-excerpt check. */
    public Recommendation preview(String priority, String report) {
        var recommendation = recommend(priority, report);
        if (RecommendationChecks.grounded(recommendation, report)) return recommendation;
        return response.recommend(UUID.randomUUID().toString(), priority, report, true);
    }

    public QualityJudge.Score score(String report, String candidateJson) {
        return quality.score(UUID.randomUUID().toString(), report, candidateJson);
    }
}

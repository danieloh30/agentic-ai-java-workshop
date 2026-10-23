package com.incidentmanagement.service;

import com.incidentmanagement.agents.*;
import com.incidentmanagement.model.Recommendation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/** Invocation lifecycle only. Prompts and AI behavior remain in declarative agent interfaces. */
@ApplicationScoped
public class ModelInvocations {
    @Inject ResponseWorkflow response;
    @Inject QualityWorkflow quality;

    public Recommendation recommend(String priority, String report) {
        return response.recommend(UUID.randomUUID().toString(), priority, report);
    }

    public QualityJudge.Score score(String report, String candidateJson) {
        return quality.score(UUID.randomUUID().toString(), report, candidateJson);
    }
}

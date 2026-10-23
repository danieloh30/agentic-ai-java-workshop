package com.incidentmanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentmanagement.agents.*;
import com.incidentmanagement.model.*;
import com.incidentmanagement.policy.RecommendationChecks;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class EvaluationService {
    public record Sample(String id, String priority, String report, String expectedAction) {}

    public record Result(
            String id,
            Recommendation output,
            boolean contractPass,
            boolean grounded,
            boolean expectedAction,
            QualityJudge.Score judge) {}

    @Inject ModelInvocations agent;
    @Inject ModelInvocations judge;
    @Inject ObjectMapper mapper;
    @Inject EvaluationRepository repository;

    @ConfigProperty(name = "quarkus.langchain4j.openai.chat-model.model-name")
    String model;

    // Calls models outside transactions; a failed run is not recorded as a completed evaluation.
    public EvaluationRun run(String label) throws Exception {
        if (label == null || !label.matches("[a-zA-Z0-9 -]{1,60}"))
            throw new WebApplicationException(
                    "Label required (1–60 letters/digits/spaces/hyphens)", 400);
        byte[] bytes;
        try (var input = getClass().getResourceAsStream("/evaluation/cases.json")) {
            bytes = input.readAllBytes();
        }
        var cases = mapper.readValue(bytes, Sample[].class);
        var results = new ArrayList<Result>();
        long start = System.nanoTime();
        for (var sample : cases) {
            var output = agent.recommend(sample.priority(), sample.report());
            var score = judge.score(sample.report(), mapper.writeValueAsString(output));
            if (score.groundedness() < 1
                    || score.groundedness() > 5
                    || score.actionability() < 1
                    || score.actionability() > 5
                    || score.reason() == null
                    || score.reason().isBlank())
                throw new IllegalStateException("Judge returned invalid rubric scores");
            results.add(
                    new Result(
                            sample.id(),
                            output,
                            RecommendationChecks.violations(output).isEmpty(),
                            RecommendationChecks.grounded(output, sample.report()),
                            sample.expectedAction().equals(output.action()),
                            score));
        }
        var run = new EvaluationRun();
        run.label = label;
        run.rubric = "apex-v1";
        run.model = model;
        run.createdAt = Instant.now();
        run.datasetHash =
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        try (var input = getClass().getResourceAsStream("/skills/incident-response/SKILL.md")) {
            run.skillHash =
                    HexFormat.of()
                            .formatHex(
                                    MessageDigest.getInstance("SHA-256")
                                            .digest(input.readAllBytes()));
        }
        run.elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        run.results = mapper.writeValueAsString(results);
        return repository.save(run);
    }

    @ApplicationScoped
    public static class EvaluationRepository {
        @Transactional
        public EvaluationRun save(EvaluationRun run) {
            run.persist();
            return run;
        }

        @Transactional
        public List<EvaluationRun> all() {
            return EvaluationRun.list("order by id desc");
        }
    }
}

package com.incidentmanagement;

import static io.restassured.RestAssured.given;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentmanagement.agents.*;
import com.incidentmanagement.model.*;
import com.incidentmanagement.policy.*;
import com.incidentmanagement.service.*;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.*;

@QuarkusTest
@QuarkusTestResource(ScriptedModel.class)
class ResponseTest {
    static final String REPORT = "Checkout 503 errors after release 2.4. No checks have run.";
    static final String VALID =
            "{\"action\":\"INVESTIGATE\",\"rationale\":\"Check release"
                    + " timing\",\"evidence\":[\"Checkout 503"
                    + " errors\"],\"verificationChecks\":[\"Compare error rate with"
                    + " baseline\"],\"requiresApproval\":true}";
    @Inject ModelInvocations agent;
    @Inject ResponseStore store;
    @Inject ResponseWorker worker;
    @Inject ObjectMapper mapper;
    @Inject EvaluationService evaluations;

    @BeforeEach
    void reset() {
        ScriptedModel.reset();
    }

    String id() {
        return UUID.randomUUID().toString();
    }

    Recommendation valid() throws Exception {
        return mapper.readValue(VALID, Recommendation.class);
    }

    ResponseRequest waiting() throws Exception {
        var r = store.create(id(), 1L, REPORT);
        store.claim();
        store.complete(r.id, 1, valid());
        return store.get(r.id);
    }

    @Test
    void nativeSkillToolRoundTrip() throws Exception {
        ScriptedModel.activateSkill();
        ScriptedModel.text(VALID);
        var output = agent.recommend("P2", REPORT);
        assertEquals("INVESTIGATE", output.action());
        assertEquals(2, ScriptedModel.requests.size());
        assertTrue(ScriptedModel.requests.getFirst().contains("activate_skill"));
        String skillResult = "";
        for (var message : mapper.readTree(ScriptedModel.requests.getLast()).get("messages")) {
            if ("tool".equals(message.path("role").asText())
                    && "skill-1".equals(message.path("tool_call_id").asText())) {
                skillResult = message.path("content").asText();
            }
        }
        assertTrue(skillResult.contains("Use the supplied incident facts only"));
        assertTrue(ScriptedModel.replies.isEmpty());
    }

    @Test
    void apiRejectsInventedEvidence() {
        ScriptedModel.text(VALID.replace("Checkout 503 errors", "CPU is 99%"));
        given().contentType("application/json")
                .body(Map.of("priority", "P2", "report", REPORT))
                .post("/api/preview")
                .then()
                .statusCode(422);
    }

    @Test
    void duplicateSubmissionPreservesRequestAndRejectsDifferentPayload() {
        String id = id();
        store.create(id, 1L, REPORT);
        assertEquals(id, store.create(id, 1L, REPORT).id);
        assertEquals(
                409,
                assertThrows(
                                WebApplicationException.class,
                                () -> store.create(id, 1L, "different report"))
                        .getResponse()
                        .getStatus());
        store.claim();
        store.fail(id, 1);
    }

    @Test
    void asynchronousHttpSubmissionReturns202AndWorkerPersistsProposal() {
        String id = id();
        given().contentType("application/json")
                .body(Map.of("requestId", id, "incidentId", 1, "report", REPORT))
                .post("/api/responses")
                .then()
                .statusCode(202)
                .header("Location", endsWith("/api/responses/" + id))
                .body("status", equalTo("QUEUED"));
        assertTrue(ScriptedModel.requests.isEmpty());
        ScriptedModel.text(VALID);
        worker.processNext();
        assertEquals("WAITING_APPROVAL", store.get(id).status);
        assertEquals(1, store.get(id).attempts);
    }

    @Test
    void approvalIsIdempotentAndOppositeOrStaleDecisionConflicts() throws Exception {
        var r = waiting();
        assertEquals(
                409,
                assertThrows(WebApplicationException.class, () -> store.decide(r.id, "wrong", true))
                        .getResponse()
                        .getStatus());
        assertEquals("APPROVED", store.decide(r.id, r.proposalHash, true).status);
        assertEquals("APPROVED", store.decide(r.id, r.proposalHash, true).status);
        assertEquals(
                409,
                assertThrows(
                                WebApplicationException.class,
                                () -> store.decide(r.id, r.proposalHash, false))
                        .getResponse()
                        .getStatus());
        assertEquals(1, store.get(r.id).attempts);
        assertTrue(ScriptedModel.requests.isEmpty());
    }

    @Test
    void waitingProposalSurvivesRecoveryWithoutReplanning() throws Exception {
        var r = waiting();
        store.recover(Instant.now().plusSeconds(999));
        var saved = store.get(r.id);
        assertEquals("WAITING_APPROVAL", saved.status);
        assertEquals(r.proposalHash, saved.proposalHash);
        assertEquals(r.proposal, saved.proposal);
        assertEquals(1, saved.attempts);
        assertTrue(ScriptedModel.requests.isEmpty());
    }

    @Test
    void contractAndGroundingChecksAreIndependent() throws Exception {
        var valid = valid();
        assertTrue(RecommendationChecks.violations(valid).isEmpty());
        assertTrue(RecommendationChecks.grounded(valid, REPORT));
        assertFalse(RecommendationChecks.grounded(valid, "Different incident"));
        assertFalse(
                RecommendationChecks.violations(
                                new Recommendation(
                                        "CLOSE", "done", List.of("fact"), List.of("check"), false))
                        .isEmpty());
        assertFalse(
                RecommendationChecks.violations(
                                new Recommendation(
                                        "INVESTIGATE",
                                        "reason",
                                        List.of("fact"),
                                        List.of(" "),
                                        true))
                        .isEmpty());
    }

    @Test
    void fixedDatasetEvaluationPersistsSeparateChecksAndJudgeScores() throws Exception {
        for (int i = 0; i < 3; i++) {
            ScriptedModel.text(VALID);
            ScriptedModel.text(
                    "{\"groundedness\":4,\"actionability\":3,\"reason\":\"Needs a precise baseline"
                            + " window\"}");
        }
        var run = evaluations.run("baseline");
        assertNotNull(run.id);
        assertEquals("apex-v1", run.rubric);
        assertEquals(64, run.datasetHash.length());
        var results = mapper.readTree(run.results);
        assertEquals(3, results.size());
        assertTrue(results.get(0).get("contractPass").asBoolean());
        assertFalse(results.get(1).get("expectedAction").asBoolean());
        assertFalse(results.get(1).get("grounded").asBoolean());
        assertEquals(3, results.get(0).get("judge").get("actionability").asInt());
        assertEquals(6, ScriptedModel.requests.size());
        assertTrue(ScriptedModel.requests.get(1).contains("apex-v1"));
    }

    @Test
    void invalidJudgeScoreDoesNotPersistCompletedRun() {
        ScriptedModel.text(VALID);
        ScriptedModel.text("{\"groundedness\":9,\"actionability\":3,\"reason\":\"bad scale\"}");
        assertThrows(IllegalStateException.class, () -> evaluations.run("invalid"));
    }

    @Test
    void invalidInputDoesNotCallModel() {
        given().contentType("application/json")
                .body(Map.of("priority", "P2", "report", " "))
                .post("/api/preview")
                .then()
                .statusCode(400);
        assertTrue(ScriptedModel.requests.isEmpty());
    }

    // TODO Ex19: Add the unrelated-evidence regression from the guide here.

    @Test
    void independentRecommendationsDoNotShareIncidentHistory() {
        ScriptedModel.text(VALID);
        agent.recommend("P2", "FIRST_INCIDENT_ONLY: Checkout 503 errors");
        ScriptedModel.text(VALID);
        agent.recommend("P2", "SECOND_INCIDENT_ONLY: Checkout 503 errors");
        String last = ScriptedModel.requests.getLast();
        assertTrue(last.contains("SECOND_INCIDENT_ONLY"));
        assertFalse(last.contains("FIRST_INCIDENT_ONLY"));
    }

    @Test
    void dashboardHasAllSixExercises() {
        given().get("/")
                .then()
                .statusCode(200)
                .body(
                        containsString("15–16"),
                        containsString("17–18"),
                        containsString("19–20"),
                        containsString("75 minutes"));
    }
}

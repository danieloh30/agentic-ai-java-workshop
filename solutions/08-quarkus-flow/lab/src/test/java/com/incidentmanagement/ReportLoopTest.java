package com.incidentmanagement;

import static io.restassured.RestAssured.given;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.incidentmanagement.agentic.workflow.BuilderReportFlow;
import com.incidentmanagement.agentic.workflow.IncidentReportFlow;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.IncidentStatus;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

@QuarkusTest
@QuarkusTestResource(ScriptedModel.class)
class ReportLoopTest {
    @Inject IncidentReportFlow flow;
    @Inject BuilderReportFlow builder;

    @BeforeEach
    void reset() {
        ScriptedModel.requests.clear();
        ScriptedModel.replies.clear();
    }

    @Test
    void firstPassPreservesTheDashboardContract() {
        ScriptedModel.replies.add("A fact-grounded report");
        ScriptedModel.replies.add("{\"score\":8,\"feedback\":\"Ready for operator review\"}");
        var result = flow.generateReport(incident("auth-service", null));
        assertEquals(Set.of("report", "score", "feedback", "iteration"), result.keySet());
        assertEquals("A fact-grounded report", result.get("report"));
        assertEquals(8, result.get("score"));
        assertEquals("Ready for operator review", result.get("feedback"));
        assertEquals(1, result.get("iteration"));
        assertEquals(2, ScriptedModel.requests.size());
    }

    @Test
    void restEndpointAndGenerateReportButtonRemainAvailable() {
        ScriptedModel.replies.add("Report for incident one");
        ScriptedModel.replies.add("{\"score\":9,\"feedback\":\"Complete\"}");
        given().post("/incident-report/1")
                .then()
                .statusCode(200)
                .body(
                        "report",
                        equalTo("Report for incident one"),
                        "score",
                        equalTo(9),
                        "iteration",
                        equalTo(1));
        given().get("/js/app.js").then().statusCode(200).body(containsString("Generate Report"));
    }

    @Test
    void missingIncidentDoesNotInvokeTheModel() {
        given().post("/incident-report/99999").then().statusCode(404);
        assertTrue(ScriptedModel.requests.isEmpty());
    }

    @Test
    void repeatedRequestsDoNotShareIncidentHistory() {
        ScriptedModel.replies.add("First report");
        ScriptedModel.replies.add("{\"score\":8,\"feedback\":\"First review\"}");
        flow.generateReport(incident("FIRST_INCIDENT_ONLY", "first incident"));
        ScriptedModel.replies.add("Second report");
        ScriptedModel.replies.add("{\"score\":8,\"feedback\":\"Second review\"}");
        flow.generateReport(incident("SECOND_INCIDENT_ONLY", "second incident"));
        assertTrue(ScriptedModel.requests.get(2).contains("SECOND_INCIDENT_ONLY"));
        assertFalse(ScriptedModel.requests.get(2).contains("FIRST_INCIDENT_ONLY"));
    }

    @Test
    void optionalBuilderHasTheSameFirstPassContract() {
        ScriptedModel.replies.add("Builder report");
        ScriptedModel.replies.add("{\"score\":8,\"feedback\":\"Reviewed\"}");
        var result = builder.generateReport(incident("auth-service", "restarts"));
        assertEquals(Set.of("report", "score", "feedback", "iteration"), result.keySet());
        assertEquals("Builder report", result.get("report"));
        assertEquals(8, result.get("score"));
        assertEquals(1, result.get("iteration"));
        assertEquals("Reviewed", result.get("feedback"));
    }

    @Test
    void builderRequestsAlsoKeepSeparateIncidentHistory() {
        ScriptedModel.replies.add("First builder report");
        ScriptedModel.replies.add("{\"score\":8,\"feedback\":\"First review\"}");
        builder.generateReport(incident("FIRST_BUILDER_ONLY", "first"));
        ScriptedModel.replies.add("Second builder report");
        ScriptedModel.replies.add("{\"score\":8,\"feedback\":\"Second review\"}");
        builder.generateReport(incident("SECOND_BUILDER_ONLY", "second"));
        assertTrue(ScriptedModel.requests.get(2).contains("SECOND_BUILDER_ONLY"));
        assertFalse(ScriptedModel.requests.get(2).contains("FIRST_BUILDER_ONLY"));
    }

    @Test
    void missingFeedbackHasAStableEmptyStringInTheResponse() {
        ScriptedModel.replies.add("Report without feedback");
        ScriptedModel.replies.add("{\"score\":8,\"feedback\":null}");
        assertEquals("", flow.generateReport(incident("auth-service", "restarts")).get("feedback"));
    }

    private IncidentInfo incident(String system, String description) {
        var i = new IncidentInfo();
        i.system = system;
        i.service = "user-login";
        i.priority = "P1";
        i.description = description;
        i.status = IncidentStatus.OPEN;
        return i;
    }
}

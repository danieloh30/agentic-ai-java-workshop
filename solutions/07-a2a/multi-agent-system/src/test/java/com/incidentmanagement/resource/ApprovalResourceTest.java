package com.incidentmanagement.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class ApprovalResourceTest {

    @Test
    void approvalUiUsesDecisionsAcceptedByApi() {
        given()
                .when().get("/js/app.js")
                .then()
                .statusCode(200)
                .body(containsString("handleProposalDecision(${proposal.id}, 'RESOLVE_INCIDENT')"))
                .body(not(containsString("handleProposalDecision(${proposal.id}, 'KEEP_AT_TEAM')")));
    }

    @Test
    void invalidDecisionReturnsBadRequest() {
        given()
                .contentType("application/json")
                .body("{\"decision\":\"KEEP_AT_TEAM\"}")
                .when().post("/api/approvals/1/decide")
                .then()
                .statusCode(400)
                .body("error", containsString("RESOLVE_INCIDENT or ESCALATE_INCIDENT"));
    }
}

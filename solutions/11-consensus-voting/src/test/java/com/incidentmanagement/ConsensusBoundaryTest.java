package com.incidentmanagement;

import com.incidentmanagement.agentic.workflow.*;
import com.incidentmanagement.model.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.restassured.RestAssured.given;

@QuarkusTest @QuarkusTestResource(ScriptedModel.class)
class ConsensusBoundaryTest {
    @Inject ConsensusWorkflow workflow;
    @Test void disagreementDoesNotTurnConfidenceIntoAWinningVote() {
        var result=ConsensusPolicy.tally(List.of(new Vote(RecommendedAction.ROLLBACK,99,"Reliability"),
                new Vote(RecommendedAction.SCALE_UP,80,"Customer"),new Vote(RecommendedAction.MONITOR,70,"Risk")));
        assertTrue(result.requiresReview()); assertNull(result.decision()); assertEquals(3,result.votes().size());
        given().get("/incident-consensus/fixtures/disagreement").then().statusCode(200);
    }
    @Test void twoOfThreeIsSufficientWithoutUnanimity() {
        var result=ConsensusPolicy.tally(List.of(new Vote(RecommendedAction.ROLLBACK,70,"Reliability"),
                new Vote(RecommendedAction.ROLLBACK,75,"Customer"),new Vote(RecommendedAction.MONITOR,99,"Risk")));
        assertFalse(result.requiresReview()); assertEquals(RecommendedAction.ROLLBACK,result.decision());
        assertEquals(2,result.agreement()); assertFalse(result.unanimous());
    }
    @Test void invalidAndMissingBallotsRequireReview() {
        assertTrue(ConsensusPolicy.tally(List.of()).requiresReview());
        assertTrue(ConsensusPolicy.tally(List.of(new Vote(RecommendedAction.ROLLBACK,101,"Invalid"),
                new Vote(RecommendedAction.ROLLBACK,80,"Valid"),new Vote(RecommendedAction.ROLLBACK,80,"Valid"))).requiresReview());
    }
    @Test void declarativeVotersProduceInspectableBallots() {
        ScriptedModel.replies.clear();
        for(int i=0;i<3;i++) ScriptedModel.replies.add("{\"action\":\"ROLLBACK\",\"confidence\":80,\"rationale\":\"Recent change\"}");
        IncidentInfo incident=new IncidentInfo();incident.system="auth-service";incident.service="login";incident.priority="1";
        var result=workflow.decide(incident,"Outage after deployment");
        assertEquals(RecommendedAction.ROLLBACK,result.decision()); assertEquals(3,result.agreement());
    }
}

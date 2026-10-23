package com.incidentmanagement;

import com.incidentmanagement.messaging.*;
import com.incidentmanagement.model.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.util.List;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest @QuarkusTestResource(ScriptedModel.class)
class EventBoundaryTest {
    @org.junit.jupiter.api.BeforeEach
    void resetScriptedModel() { ScriptedModel.replies.clear(); ScriptedModel.requests.clear(); }

    @Inject ResolutionStore store;
    @Inject ResolutionSink sink;
    @Inject IncidentEventConsumer consumer;
    @Inject com.incidentmanagement.agentic.workflow.IncidentResolutionWorkflow workflow;

    @Test void duplicatesAreIdempotentAndOldResultsCannotReplaceLatestRequest() {
        var first=store.register(2,"Older report");
        var result=new ResolutionEvent(first.requestId(),2,"AWAITING_REVIEW","P1","Recommend investigation");
        sink.consume(result);sink.consume(result);
        assertEquals(1,store.count(first.requestId()));
        assertEquals(result,store.completed(first.requestId()));
        if (ReplayPolicy.reuseCompleted()) assertEquals(result,consumer.process(first));
        var latest=store.register(2,"New report");
        sink.consume(result);
        assertEquals(latest.requestId(),store.get(2).requestId());
        assertEquals("PUBLISHED",store.get(2).processingStatus());
        assertThrows(IllegalArgumentException.class,()->sink.consume(
                new ResolutionEvent(latest.requestId(),4,"AWAITING_REVIEW","P4","Wrong incident")));
        sink.consume(new ResolutionEvent(latest.requestId(),2,"FAILED","","Evidence unavailable"));
        assertEquals("FAILED",store.get(2).processingStatus());
        QuarkusTransaction.requiringNew().run(()-> {
            IncidentInfo incident=IncidentInfo.findById(2L);
            assertEquals(IncidentStatus.IN_PROGRESS,incident.status);
        });
    }
    @Test void scriptedAgentsReturnACorrelatedRecommendation() {
        var event=store.register(4,"EU asset latency");
        ScriptedModel.replies.clear();ScriptedModel.replies.addAll(List.of("P4: regional latency","Investigate regional cache"));
        var result=workflow.process(event);
        assertEquals(event.requestId(),result.requestId());
        assertEquals("AWAITING_REVIEW",result.processingStatus());
        assertEquals("Investigate regional cache",result.resolution());
    }

    @Test void httpSubmissionTravelsThroughKafkaAndIsReadableAfterCompletion() throws Exception {
        ScriptedModel.replies.clear();
        ScriptedModel.replies.addAll(List.of("P4: regional latency","Investigate regional cache"));
        String requestId=given().contentType("text/plain").body("EU assets are slow")
                .post("/incident-events/publish/4").then().statusCode(200).extract().path("requestId");
        long deadline=System.nanoTime()+java.time.Duration.ofSeconds(30).toNanos();
        ResolutionEvent outcome=null;
        while(System.nanoTime()<deadline) {
            outcome=store.completed(requestId);
            if(outcome!=null) break;
            Thread.sleep(50);
        }
        assertNotNull(outcome,"Kafka pipeline must persist a terminal outcome");
        assertTrue(List.of("AWAITING_REVIEW","FAILED").contains(outcome.processingStatus()));
        given().get("/incident-events/resolutions/4").then().statusCode(200)
                .body("requestId",org.hamcrest.Matchers.equalTo(requestId));
        given().post("/incident-events/replay/"+requestId).then().statusCode(200);
        assertEquals(1,store.count(requestId));
    }
}

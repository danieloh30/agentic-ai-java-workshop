package com.incidentmanagement;

import com.incidentmanagement.memory.PersistentChatMemoryStore;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest @QuarkusTestResource(ScriptedModel.class)
class PartTwoDashboardTest {
    @Inject PersistentChatMemoryStore memoryStore;
    @Test void dashboardExposesAllFourCapabilitiesInOneApplication() {
        given().get("/").then().statusCode(200).body(containsString("workshop-mode"),
                containsString("Planning / MCP"),containsString("Memory"),
                containsString("Voting"),containsString("Events / Replay"));
    }
    @Test void unsupportedModeDoesNotRunAnAgent() {
        given().queryParam("mode","UNKNOWN").queryParam("report","Report")
                .post("/incident-management/process/2").then().statusCode(400);
    }
    @Test void memoryModeRoutesOnlyTheOperatorMessageToItsIncidentConversation() {
        memoryStore.deleteMessages(8);ScriptedModel.replies.clear();ScriptedModel.requests.clear();
        ScriptedModel.replies.add("Inspect the reported rate limiter");
        given().queryParam("mode","MEMORY").queryParam("report","Rate limiter rejects legitimate traffic")
                .post("/incident-management/process/8").then().statusCode(200)
                .body(containsString("Inspect the reported rate limiter"));
        assertEquals(3,memoryStore.getMessages(8).size());
        assertTrue(ScriptedModel.requests.getLast().contains("Rate limiter rejects legitimate traffic"));
        assertTrue(memoryStore.getMessages("default").isEmpty());
        memoryStore.deleteMessages(8);
    }
}

package com.incidentmanagement;

import com.incidentmanagement.evidence.DeploymentEvidenceClient;
import io.quarkus.test.junit.*;
import io.quarkus.test.common.QuarkusTestResource;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest @TestProfile(EvidenceUnavailableTest.Unavailable.class)
@QuarkusTestResource(ScriptedModel.class)
class EvidenceUnavailableTest {
    @org.junit.jupiter.api.BeforeEach
    void resetScriptedModel() { ScriptedModel.replies.clear(); ScriptedModel.requests.clear(); }

    @Inject DeploymentEvidenceClient evidence;
    @Test void unavailableFixtureCrossesMcpBoundaryAndFailsClosed() {
        ScriptedModel.requests.clear();
        assertEquals("unavailable",evidence.fetch("auth-service").status());
        assertTrue(ScriptedModel.requests.isEmpty());
    }
    public static class Unavailable implements QuarkusTestProfile {
        @Override public Map<String,String> getConfigOverrides() {
            return Map.of("workshop.evidence.scenario","unavailable");
        }
    }
}

package com.incidentmanagement;

import com.incidentmanagement.agentic.workflow.*;
import com.incidentmanagement.evidence.*;
import com.incidentmanagement.model.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(ScriptedModel.class)
class PlanExecuteTest {
    @Inject DeploymentEvidenceClient evidence;
    @Inject PlanExecuteFlow flow;
    @BeforeEach void reset() { ScriptedModel.replies.clear(); ScriptedModel.requests.clear(); }

    @Test void mcpReturnsCorrelatedFixtureAndFailsClosed() {
        assertTrue(evidence.fetch("auth-service").usableFor("auth-service"));
        assertEquals("unavailable", evidence.fetch("").status());
        assertTrue(ScriptedModel.requests.isEmpty());
    }
    @Test void invalidPrerequisitesAndDuplicateStepsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> PlanValidator.validate(
                new ExecutionPlan(List.of(ResolutionStep.VERIFY, ResolutionStep.MITIGATE), "bad"), false, false));
        assertThrows(IllegalArgumentException.class, () -> PlanValidator.validate(
                new ExecutionPlan(List.of(ResolutionStep.DIAGNOSE, ResolutionStep.DIAGNOSE), "duplicate"), false, false));
        assertDoesNotThrow(() -> PlanValidator.validate(
                new ExecutionPlan(List.of(ResolutionStep.MITIGATE, ResolutionStep.VERIFY), "replan"), true, false));
    }
    @Test void scriptedPlanUsesMcpEvidenceAndKeepsHistory() {
        ScriptedModel.replies.addAll(List.of(
                "{\"steps\":[\"DIAGNOSE\",\"MITIGATE\",\"VERIFY\",\"COMMUNICATE\"],\"rationale\":\"Investigate the memory change\"}",
                "Memory limit reduced in fixture release", "Recommend restoring the previous memory limit",
                "READY: recommendation addresses the diagnosis; verify in production", "Tell operators to verify recovery",
                "{\"recommendationReady\":true,\"feedback\":\"Recommendation complete\"}"));
        IncidentInfo incident = new IncidentInfo(); incident.system="auth-service"; incident.service="user-login";
        incident.priority="1"; incident.description="OOMKilled pods";
        var result = flow.resolve(incident, 2, "Repeated restarts");
        assertEquals("RECOMMENDATION_READY", result.get("outcome"));
        assertEquals(1, ((List<?>) result.get("history")).size());
        assertTrue(ScriptedModel.requests.getFirst().contains("workshop-release-42"));
        assertTrue(ScriptedModel.replies.isEmpty());
    }
    @Test void rejectedPlansExhaustBudgetWithoutRunningSpecialists() {
        for(int i=0;i<3;i++) ScriptedModel.replies.add("{\"steps\":[\"VERIFY\"],\"rationale\":\"Invalid prerequisite\"}");
        IncidentInfo incident = new IncidentInfo(); incident.system="auth-service"; incident.service="login";
        incident.priority="1"; incident.description="Restarts";
        var result=flow.resolve(incident,2,"Investigate");
        assertEquals("NEEDS_OPERATOR_REVIEW",result.get("outcome"));
        assertEquals(3,result.get("iteration"));
        assertEquals(3,((List<?>) result.get("history")).size());
        assertEquals(3,ScriptedModel.requests.size());
        assertTrue(ScriptedModel.requests.getLast().contains("Plan rejected before execution"));
    }
    @Test void missingEvidenceStopsBeforeAnyModelCall() {
        IncidentInfo incident = new IncidentInfo(); incident.system="";
        assertEquals("EVIDENCE_UNAVAILABLE", flow.resolve(incident, 2, "report").get("outcome"));
        assertTrue(ScriptedModel.requests.isEmpty());
    }
}

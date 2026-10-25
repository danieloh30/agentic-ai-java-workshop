package com.incidentmanagement;

import com.incidentmanagement.agentic.agents.PlanGenerationAgent;
import com.incidentmanagement.evidence.DeploymentEvidenceClient;
import com.incidentmanagement.agentic.workflow.PlanValidator;
import com.incidentmanagement.model.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
@QuarkusTest @QuarkusTestResource(ScriptedModel.class)
class PlanExecuteTest {
    @Inject DeploymentEvidenceClient evidence;
    @Inject PlanGenerationAgent planGenerationAgent;
    @Test void suppliedMcpAndPlanBoundaryWork() {
        assertTrue(evidence.fetch("auth-service").usableFor("auth-service"));
        assertThrows(IllegalArgumentException.class, () -> PlanValidator.validate(
                new ExecutionPlan(List.of(ResolutionStep.VERIFY), "bad"), false, false));
    }

    @Test void planGeneratorReturnsAValidatedStructuredPlan() {
        ScriptedModel.replies.clear();
        ScriptedModel.requests.clear();
        ScriptedModel.replies.add("""
                {"steps":["DIAGNOSE","MITIGATE","VERIFY"],"rationale":"Investigate the restarts"}
                """);
        var plan = planGenerationAgent.plan(
                "auth-service", "user-login", "P1", "Pods are restarting", 2,
                "OOMKilled every five minutes", "fixture-deployment-evidence", "");
        assertEquals(List.of(ResolutionStep.DIAGNOSE, ResolutionStep.MITIGATE, ResolutionStep.VERIFY), plan.steps());
        assertEquals("Investigate the restarts", plan.rationale());
        assertDoesNotThrow(() -> PlanValidator.validate(plan, false, false));
        assertTrue(ScriptedModel.requests.getLast().contains("OOMKilled every five minutes"));
        assertTrue(ScriptedModel.requests.getLast().contains("fixture-deployment-evidence"));
        assertTrue(ScriptedModel.replies.isEmpty());
    }
}

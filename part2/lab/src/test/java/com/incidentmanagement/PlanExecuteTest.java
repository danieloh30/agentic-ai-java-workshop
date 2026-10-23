package com.incidentmanagement;
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
    @Test void suppliedMcpAndPlanBoundaryWork() {
        assertTrue(evidence.fetch("auth-service").usableFor("auth-service"));
        assertThrows(IllegalArgumentException.class, () -> PlanValidator.validate(
                new ExecutionPlan(List.of(ResolutionStep.VERIFY), "bad"), false, false));
    }
}

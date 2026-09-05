package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.model.IncidentAnalysisResults;
import com.incidentmanagement.model.IncidentInfo;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class IncidentSupervisorAgentTest {

    @Test
    void preservesTheIncidentReportAcrossSupervisorSubAgentCalls() {
        IncidentInfo incident = new IncidentInfo();
        incident.system = "payment-gateway";
        incident.service = "checkout-api";
        incident.priority = 2;
        incident.description = "Intermittent 503 errors";

        String report = "ORIGINAL_REPORT_MARKER";
        String request = IncidentSupervisorAgent.request(
                incident, 1,
                new IncidentAnalysisResults("SEVERITY_HIGH", "IMPACT_HIGH", "ESCALATION_NOT_REQUIRED"),
                report);

        assertTrue(request.contains("Incident Report: " + report));
        assertTrue(request.contains("pass the exact Incident Report below as triageReport"));

        Method triageMethod = Arrays.stream(TriageAgent.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("processTriage"))
                .findFirst().orElseThrow();
        assertEquals("triageReport", triageMethod.getParameters()[2].getName());

        Method escalationMethod = Arrays.stream(EscalationAgent.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("processEscalation"))
                .findFirst().orElseThrow();
        assertEquals(6, escalationMethod.getParameterCount());
    }
}

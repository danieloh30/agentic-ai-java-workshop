package com.incidentmanagement.agentic.agents;

import static org.junit.jupiter.api.Assertions.*;
import dev.langchain4j.agentic.AgenticServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Exercises the deployed WildFly service using the real Quarkus caller's dependency graph. */
@EnabledIfSystemProperty(named = "wildfly.url", matches = ".+")
class WildFlyInteropTest {
    @Test
    void existingCallerContractInvokesWildFly() {
        var remote = AgenticServices.a2aBuilder(System.getProperty("wildfly.url"), ImpactAgent.class).build();
        String result = remote.assessImpact("payment-gateway", "checkout-api", 1,
                "503 outage; all endpoints fail\nCustomer checkout is unavailable.");
        assertTrue(result.contains("Estimated Impact: $36,000"), result);
        assertTrue(result.contains("Justification:"), result);
    }
}

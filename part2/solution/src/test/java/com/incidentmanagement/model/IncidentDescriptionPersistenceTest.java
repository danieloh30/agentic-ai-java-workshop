package com.incidentmanagement.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(IncidentDescriptionPersistenceTest.PersistenceProfile.class)
class IncidentDescriptionPersistenceTest {

    @Test
    void commitsAndReloadsLongSummariesOnRepeatedProcessing() {
        Long incidentId = 2L;
        // Use the real seed schema: import.sql recreates the table after Hibernate's DDL.
        OriginalIncident original = QuarkusTransaction.requiringNew().call(() -> {
            IncidentInfo incident = IncidentInfo.findById(incidentId);
            assertNotNull(incident);
            return new OriginalIncident(incident.description, incident.status);
        });

        try {
            for (int run = 1; run <= 2; run++) {
                String summary = "Plan: [DIAGNOSE, MITIGATE, VERIFY, COMMUNICATE] — Run " + run
                        + "\n\nDiagnosis: " + "Authentication pods exhausted memory. ".repeat(100)
                        + "\n\nMitigation: " + "Increase memory limits and restart pods. ".repeat(100)
                        + "\n\nVerification: " + "Login checks passed. ".repeat(100)
                        + "\n\nCommunication: Stakeholders notified.\n\nResolved: true";
                assertTrue(summary.length() > 255);

                QuarkusTransaction.requiringNew().run(() -> {
                    IncidentInfo incident = IncidentInfo.findById(incidentId);
                    incident.description = summary;
                    incident.status = IncidentStatus.RESOLVED;
                });

                // A new transaction reads the committed database value, not the managed entity.
                QuarkusTransaction.requiringNew().run(() -> {
                    IncidentInfo reloaded = IncidentInfo.findById(incidentId);
                    assertEquals(summary, reloaded.description);
                    assertEquals(IncidentStatus.RESOLVED, reloaded.status);
                });
            }
        } finally {
            QuarkusTransaction.requiringNew().run(() -> {
                IncidentInfo incident = IncidentInfo.findById(incidentId);
                incident.description = original.description();
                incident.status = original.status();
            });
        }
    }

    private record OriginalIncident(String description, IncidentStatus status) {}

    public static class PersistenceProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            // No LLM calls are needed to exercise persistence.
            return Map.of("quarkus.langchain4j.openai.api-key", "test-key");
        }
    }
}

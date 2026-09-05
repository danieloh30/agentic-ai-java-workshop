package com.incidentmanagement.agentic.agents;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ReportAccuracyGuardrailTest {

    @Test
    void drafterAndCriticRejectUnsupportedIncidentFacts() throws NoSuchMethodException {
        String drafterRules = Arrays.toString(ReportDrafterAgent.class
                .getDeclaredMethod("draftReport", String.class, String.class, String.class,
                        String.class, String.class, String.class)
                .getAnnotation(SystemMessage.class).value());
        assertTrue(drafterRules.contains("Never invent timestamps"));
        assertTrue(drafterRules.contains("Not available in the incident record"));

        var criticMethod = ReportCriticAgent.class.getDeclaredMethod(
                "critiqueReport", String.class, String.class, String.class,
                String.class, String.class, String.class);
        String criticRules = Arrays.toString(criticMethod.getAnnotation(SystemMessage.class).value());
        String criticInput = Arrays.toString(criticMethod.getAnnotation(UserMessage.class).value());

        assertTrue(criticRules.contains("score it no higher than 6"));
        assertTrue(criticInput.contains("Description: {description}"));
        assertTrue(criticInput.contains("Status: {status}"));
        assertEquals(6, criticMethod.getParameterCount());
    }
}

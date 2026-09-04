package com.incidentmanagement.agentic.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class TriageWorkflowTest {

    @Inject
    Instance<TriageWorkflow> triageWorkflow;

    @Test
    void workflowAvailabilityMatchesExerciseProgress() throws NoSuchMethodException {
        boolean sequenceConfigured = TriageWorkflow.class
                .getMethod("processTriage", IncidentInfo.class, Integer.class, String.class)
                .isAnnotationPresent(SequenceAgent.class);
        boolean outputConfigured = TriageWorkflow.class
                .getMethod("output", String.class)
                .isAnnotationPresent(Output.class);

        assertEquals(sequenceConfigured, outputConfigured);
        assertEquals(sequenceConfigured, triageWorkflow.isResolvable());
    }
}

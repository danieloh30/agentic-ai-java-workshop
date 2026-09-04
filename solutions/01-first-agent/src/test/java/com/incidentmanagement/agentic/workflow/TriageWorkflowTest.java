package com.incidentmanagement.agentic.workflow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import com.incidentmanagement.agentic.agents.TriageAgent;
import com.incidentmanagement.model.IncidentInfo;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class TriageWorkflowTest {

    @Inject
    TriageWorkflow triageWorkflow;

    @Test
    void registersTriageAgentInsideAnAgenticSystem() throws NoSuchMethodException {
        assertNotNull(triageWorkflow);

        SequenceAgent sequenceAgent = TriageWorkflow.class
                .getMethod("processTriage", IncidentInfo.class, Integer.class, String.class)
                .getAnnotation(SequenceAgent.class);

        assertNotNull(sequenceAgent);
        assertArrayEquals(new Class<?>[] { TriageAgent.class }, sequenceAgent.subAgents());
        assertNotNull(TriageWorkflow.class.getMethod("output", String.class).getAnnotation(Output.class));
    }
}

package com.incidentmanagement.agents;

import com.incidentmanagement.model.Recommendation;

import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.service.MemoryId;

/** An agentic entry point gives each recommendation a fresh scope. */
public interface ResponseWorkflow {
    @SequenceAgent(outputKey = "responseResult", subAgents = ResponseAgent.class)
    Recommendation recommend(@MemoryId String invocationId, String priority, String report);

    @Output
    static Recommendation output(AgenticScope scope) {
        return scope.readState("recommendation", (Recommendation) null);
    }
}

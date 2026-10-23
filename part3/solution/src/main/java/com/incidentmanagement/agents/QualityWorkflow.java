package com.incidentmanagement.agents;

import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.service.MemoryId;

/** The judge has its own scope, independent of the response agent. */
public interface QualityWorkflow {
    @SequenceAgent(outputKey = "judgedQuality", subAgents = QualityJudge.class)
    QualityJudge.Score score(@MemoryId String invocationId, String report, String candidateJson);

    @Output
    static QualityJudge.Score output(AgenticScope scope) {
        return scope.readState("qualityScore", (QualityJudge.Score) null);
    }
}

package com.incidentmanagement.agentic.agents;

import dev.langchain4j.agentic.declarative.A2AClientAgent;

public interface ImpactAgent {

    @A2AClientAgent(a2aServerUrl = "http://localhost:8888/",
                     outputKey = "businessImpact",
                     description = "Impact assessment specialist that estimates business impact based on system criticality, priority, and SLA risk")
    String assessImpact(String system, String service, Integer priority, String incidentDescription);
}

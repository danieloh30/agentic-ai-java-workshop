package com.incidentmanagement.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;
import io.quarkiverse.langchain4j.mcp.runtime.McpClientName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;

/** Required evidence is fetched by workflow code, without asking an LLM to choose a tool. */
@ApplicationScoped
public class DeploymentEvidenceClient {
    @Inject @McpClientName("evidence") McpClient client;
    @Inject ObjectMapper mapper;

    public DeploymentEvidence fetch(String service) {
        try {
            // Discover metadata before calling a tool directly (including MCP parameter headers).
            if (client.listTools().stream().noneMatch(tool -> "getRecentDeployments".equals(tool.name()))) {
                throw new IllegalStateException("Required deployment tool is not advertised");
            }
            var result = client.executeTool(ToolExecutionRequest.builder()
                    .name("getRecentDeployments")
                    .arguments(mapper.writeValueAsString(Map.of("service", service))).build());
            if (result.isError()) throw new IllegalStateException("MCP tool failed");
            DeploymentEvidence evidence = mapper.readValue(result.resultText(), DeploymentEvidence.class);
            if (!evidence.usableFor(service)) throw new IllegalStateException("Evidence missing or mismatched");
            return evidence;
        } catch (Exception failure) {
            // Transport failures and invalid evidence have the same safe workflow outcome.
            return new DeploymentEvidence(service, "unavailable", "", "Required deployment evidence is unavailable");
        }
    }
}

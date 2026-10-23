package com.incidentmanagement.evidence;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Supplied MCP server, hosted alongside the dashboard to keep setup short. */
@ApplicationScoped
public class DeploymentEvidenceTool {
    @ConfigProperty(name = "workshop.evidence.scenario", defaultValue = "available")
    String scenario;

    @Tool(description = "Returns fixed workshop deployment evidence for an incident service.")
    public DeploymentEvidence getRecentDeployments(@ToolArg(description = "Incident system name") String service) {
        if (service == null || service.isBlank()) throw new IllegalArgumentException("Service is required");
        if ("unavailable".equals(scenario)) {
            return new DeploymentEvidence(service, "unavailable", "", "Fixture evidence source unavailable");
        }
        return new DeploymentEvidence(service, "available", "workshop-release-42",
                "auth-service".equals(service)
                    ? "Fixture: memory limit reduced in the latest release; no recovery checks have been observed."
                    : "Fixture: a recent release exists; no causal link or recovery has been established.");
    }
}

package com.incidentmanagement.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentmanagement.ScriptedModel;
import dev.langchain4j.mcp.client.McpClient;
import io.quarkiverse.langchain4j.mcp.runtime.McpClientName;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(ScriptedModel.class)
class DeploymentEvidenceClientTest {
    @Inject @McpClientName("evidence") McpClient realClient;
    @Inject ObjectMapper mapper;

    @Test
    void discoversBeforeExecutionAndReturnsEvidenceAcrossRepeatedRequests() {
        var calls = new ArrayList<String>();
        var client = wrapper((McpClient) Proxy.newProxyInstance(McpClient.class.getClassLoader(),
                new Class<?>[]{McpClient.class}, (proxy, method, args) -> {
                    calls.add(method.getName());
                    return method.invoke(realClient, args);
                }));
        assertTrue(client.fetch("auth-service").usableFor("auth-service"));
        assertTrue(client.fetch("auth-service").usableFor("auth-service"));
        assertEquals(List.of("listTools", "executeTool", "listTools", "executeTool"), calls);
    }

    @Test
    void discoveryFailureStopsBeforeToolExecution() {
        var calls = new ArrayList<String>();
        var client = wrapper((McpClient) Proxy.newProxyInstance(McpClient.class.getClassLoader(),
                new Class<?>[]{McpClient.class}, (proxy, method, args) -> {
                    calls.add(method.getName());
                    throw new IllegalStateException("Discovery failed");
                }));
        assertEquals("unavailable", client.fetch("auth-service").status());
        assertEquals(List.of("listTools"), calls);
    }

    @Test
    void missingRequiredToolStopsBeforeExecution() {
        var calls = new ArrayList<String>();
        var client = wrapper((McpClient) Proxy.newProxyInstance(McpClient.class.getClassLoader(),
                new Class<?>[]{McpClient.class}, (proxy, method, args) -> {
                    calls.add(method.getName());
                    if (method.getName().equals("listTools")) return List.of();
                    fail("Must not execute an unadvertised tool");
                    return null;
                }));
        assertEquals("unavailable", client.fetch("auth-service").status());
        assertEquals(List.of("listTools"), calls);
    }

    private DeploymentEvidenceClient wrapper(McpClient mcp) {
        var wrapper = new DeploymentEvidenceClient();
        wrapper.client = mcp;
        wrapper.mapper = mapper;
        return wrapper;
    }
}

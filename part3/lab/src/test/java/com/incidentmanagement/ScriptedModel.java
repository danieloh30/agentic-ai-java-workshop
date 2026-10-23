package com.incidentmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Queues real OpenAI protocol messages, including native skill tool calls. No paid calls. */
public class ScriptedModel implements QuarkusTestResourceLifecycleManager {
    private HttpServer server;
    public static final Queue<Map<String, Object>> replies = new ConcurrentLinkedQueue<>();
    public static final List<String> requests = new CopyOnWriteArrayList<>();

    public static void text(String text) {
        replies.add(Map.of("role", "assistant", "content", text));
    }

    public static void activateSkill() {
        replies.add(
                Map.of(
                        "role",
                        "assistant",
                        "tool_calls",
                        List.of(
                                Map.of(
                                        "id",
                                        "skill-1",
                                        "type",
                                        "function",
                                        "function",
                                        Map.of(
                                                "name",
                                                "activate_skill",
                                                "arguments",
                                                "{\"skill_name\":\"incident-response\"}")))));
    }

    public static void reset() {
        replies.clear();
        requests.clear();
    }

    public Map<String, String> start() {
        try {
            var mapper = new ObjectMapper();
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext(
                    "/v1/chat/completions",
                    exchange -> {
                        requests.add(
                                new String(
                                        exchange.getRequestBody().readAllBytes(),
                                        StandardCharsets.UTF_8));
                        var message = replies.poll();
                        if (message == null) {
                            exchange.sendResponseHeaders(500, -1);
                            exchange.close();
                            return;
                        }
                        byte[] body =
                                mapper.writeValueAsBytes(
                                        Map.of(
                                                "id",
                                                "fixture",
                                                "object",
                                                "chat.completion",
                                                "created",
                                                1,
                                                "model",
                                                "fixture",
                                                "choices",
                                                List.of(
                                                        Map.of(
                                                                "index",
                                                                0,
                                                                "message",
                                                                message,
                                                                "finish_reason",
                                                                message.containsKey("tool_calls")
                                                                        ? "tool_calls"
                                                                        : "stop")),
                                                "usage",
                                                Map.of(
                                                        "prompt_tokens",
                                                        10,
                                                        "completion_tokens",
                                                        10,
                                                        "total_tokens",
                                                        20)));
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, body.length);
                        exchange.getResponseBody().write(body);
                        exchange.close();
                    });
            server.start();
            return Map.of(
                    "quarkus.langchain4j.openai.base-url",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void stop() {
        if (server != null) server.stop(0);
        reset();
    }
}

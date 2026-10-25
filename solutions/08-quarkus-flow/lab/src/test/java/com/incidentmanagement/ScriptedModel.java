package com.incidentmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class ScriptedModel implements QuarkusTestResourceLifecycleManager {
    private HttpServer server;
    public static final ConcurrentLinkedQueue<String> replies = new ConcurrentLinkedQueue<>();
    public static final List<String> requests = new CopyOnWriteArrayList<>();

    @Override
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
                        String reply = replies.poll();
                        if (reply == null) {
                            exchange.sendResponseHeaders(500, -1);
                            exchange.close();
                            return;
                        }
                        byte[] body =
                                mapper.writeValueAsBytes(
                                        Map.of(
                                                "id",
                                                "scripted",
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
                                                                Map.of(
                                                                        "role",
                                                                        "assistant",
                                                                        "content",
                                                                        reply),
                                                                "finish_reason",
                                                                "stop")),
                                                "usage",
                                                Map.of(
                                                        "prompt_tokens",
                                                        1,
                                                        "completion_tokens",
                                                        1,
                                                        "total_tokens",
                                                        2)));
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, body.length);
                        exchange.getResponseBody().write(body);
                        exchange.close();
                    });
            server.start();
            return Map.of(
                    "quarkus.langchain4j.openai.base-url",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
                    "quarkus.langchain4j.openai.api-key",
                    "scripted-test-key");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void stop() {
        if (server != null) server.stop(0);
        replies.clear();
        requests.clear();
    }
}

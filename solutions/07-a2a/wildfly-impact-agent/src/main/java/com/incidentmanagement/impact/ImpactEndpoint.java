package com.incidentmanagement.impact;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** HTTP transport only; the CDI extension creates the injected @Agent implementation. */
@WebServlet(urlPatterns = {"/", "/.well-known/agent-card.json"})
public class ImpactEndpoint extends HttpServlet {
    @Inject ImpactAgent agent;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!request.getRequestURI().equals("/.well-known/agent-card.json")) {
            response.sendError(404);
            return;
        }
        try {
            response.setContentType("application/json");
            response.getWriter().write(ImpactProtocol.card(
                    System.getenv().getOrDefault("PUBLIC_AGENT_URL", "http://localhost:8888/")));
        } catch (Exception failure) {
            response.sendError(500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!request.getRequestURI().equals("/")) { response.sendError(404); return; }
        if (request.getContentType() == null || !request.getContentType().startsWith("application/json")) {
            response.sendError(415); return;
        }
        byte[] bytes = request.getInputStream().readNBytes(32769);
        if (bytes.length > 32768) { response.sendError(413); return; }
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(ImpactProtocol.handle(new String(bytes, StandardCharsets.UTF_8),
                values -> agent.assessImpact(values[0], values[1], Integer.valueOf(values[2]), values[3])));
    }
}

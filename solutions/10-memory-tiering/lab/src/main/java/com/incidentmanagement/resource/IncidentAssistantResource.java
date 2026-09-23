package com.incidentmanagement.resource;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.incidentmanagement.agentic.agents.IncidentAssistant;
import com.incidentmanagement.memory.PersistentChatMemoryStore;

import dev.langchain4j.data.message.ChatMessage;
import io.quarkus.logging.Log;

/**
 * Chat with the per-incident {@link IncidentAssistant}, and peek at the durable memory.
 * Send several messages to the same incident id and the assistant remembers the earlier
 * turns, because they are replayed from Tier 1 (the message window) — which in turn reads
 * from Tier 2 ({@link PersistentChatMemoryStore} / PostgreSQL).
 *
 * <p>The engineer's message is the request body (text/plain), e.g.
 * {@code curl -X POST .../incident-assistant/2 -H 'Content-Type: text/plain' --data 'hello'}.
 */
@Path("/incident-assistant")
@Produces(MediaType.APPLICATION_JSON)
public class IncidentAssistantResource {

    @Inject
    IncidentAssistant incidentAssistant;

    @Inject
    PersistentChatMemoryStore memoryStore;

    @POST
    @Path("/{incidentId}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Map<String, Object> chat(Integer incidentId, String message) {
        if (message == null || message.isBlank()) {
            throw new BadRequestException("Send the engineer's message as the request body (text/plain).");
        }
        Log.infof("Assistant chat for incident #%d: %s", incidentId, message);
        String reply = incidentAssistant.chat(incidentId, message);
        return Map.of("incidentId", incidentId, "reply", reply);
    }

    /** Inspect Tier 2 directly: the persisted transcript for this incident. */
    @GET
    @Path("/{incidentId}/history")
    public Map<String, Object> history(Integer incidentId) {
        List<ChatMessage> messages = memoryStore.getMessages(incidentId);
        return Map.of("incidentId", incidentId,
                "messageCount", messages.size(),
                "messages", messages.stream().map(ChatMessage::type).map(Enum::name).toList());
    }
}

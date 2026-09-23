package com.incidentmanagement.agentic.agents;

import com.incidentmanagement.memory.PersistentChatMemoryStore;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.ChatMemoryProviderSupplier;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkus.arc.Arc;

/**
 * A conversational incident assistant — a declarative agentic {@code @Agent} with its own
 * two-tier memory, wired the agentic-native way.
 *
 * <p>{@code @ChatMemoryProviderSupplier} is a {@code quarkus-langchain4j-agentic} feature:
 * a static method that hands the agent a {@link ChatMemoryProvider}. Here that provider
 * builds both tiers —
 * <ul>
 *   <li><b>Tier 1 (working memory):</b> a {@link MessageWindowChatMemory} that keeps only
 *       the last N messages actually sent to the model;</li>
 *   <li><b>Tier 2 (durable store):</b> that window reads/writes through the
 *       {@link PersistentChatMemoryStore} bean, so the full transcript survives restarts.</li>
 * </ul>
 * The {@code @MemoryId} parameter is what scopes a conversation to one incident.
 */
public interface IncidentAssistant {

    @SystemMessage("""
            You are an on-call incident assistant for an IT incident-management system.
            You help an engineer reason about ONE incident across a back-and-forth
            conversation. Remember what the engineer has already told you in this
            conversation and build on it — do not ask again for facts you were given.
            Be concise and concrete. Never invent metrics, logs, or actions that were not
            stated. If you don't have enough information, say what you'd need.
            """)
    @Agent(description = "Conversational assistant that reasons about a single incident with memory of the conversation so far.",
           outputKey = "assistantReply")
    String chat(@MemoryId Integer incidentId, @UserMessage String message);

    /**
     * Agentic memory wiring: window (Tier 1) over the durable, CDI-managed store (Tier 2).
     * We resolve the store through Arc so its {@code @Transactional} Panache methods keep
     * their interceptors.
     */
    @ChatMemoryProviderSupplier
    static ChatMemory chatMemory(Object memoryId) {
        PersistentChatMemoryStore store = Arc.container().instance(PersistentChatMemoryStore.class).get();
        return MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .chatMemoryStore(store)
                .build();
    }
}

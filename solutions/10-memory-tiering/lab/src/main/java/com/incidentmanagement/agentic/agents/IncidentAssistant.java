package com.incidentmanagement.agentic.agents;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

/**
 * A conversational incident assistant — a declarative agentic {@code @Agent} with its own
 * two-tier memory.
 *
 * <p>TODO (Exercise 10): give the assistant memory.
 * <ol>
 *   <li>Annotate {@link #chat} with a {@code @SystemMessage} (tell it to remember what the
 *       engineer already said and build on it) and with
 *       {@code @Agent(description = "...", outputKey = "assistantReply")}. The
 *       {@code @MemoryId} on {@code incidentId} is what scopes a conversation to one incident.</li>
 *   <li>Add a static method annotated {@code @ChatMemoryProviderSupplier} that returns a
 *       {@code ChatMemory}: a {@code MessageWindowChatMemory} (Tier 1 — the last N messages)
 *       built over the {@code PersistentChatMemoryStore} bean (Tier 2 — durable). Resolve the
 *       store with {@code io.quarkus.arc.Arc.container().instance(...).get()} so its
 *       {@code @Transactional} methods keep their interceptors. The supplier method signature
 *       must be {@code static ChatMemory chatMemory(Object memoryId)}.</li>
 * </ol>
 */
public interface IncidentAssistant {

    // TODO: add @SystemMessage and @Agent(...); this is the agent's one conversational method.
    String chat(@MemoryId Integer incidentId, @UserMessage String message);

    // TODO: add the @ChatMemoryProviderSupplier static method (see the class Javadoc above).
}

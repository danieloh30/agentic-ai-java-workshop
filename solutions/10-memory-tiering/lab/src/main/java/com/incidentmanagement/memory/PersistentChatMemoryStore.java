package com.incidentmanagement.memory;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

/**
 * Tier 2 — the durable backing store for chat memory.
 *
 * <p>Providing a {@code ChatMemoryStore} CDI bean is all it takes: quarkus-langchain4j uses
 * it in place of its default in-memory store, so the {@code MessageWindowChatMemory} (Tier 1)
 * reads and writes through PostgreSQL instead.
 *
 * <p>TODO (Exercise 10): implement the three methods against {@link ChatMemoryEntity}
 * (one row per {@code memoryId}, transcript stored as JSON). Make each method
 * {@code @Transactional} and convert the {@code memoryId} to a String key.
 * <ul>
 *   <li>{@code getMessages}: find the row; if none, return {@code List.of()}; else
 *       {@code ChatMessageDeserializer.messagesFromJson(entity.messagesJson)}.</li>
 *   <li>{@code updateMessages}: find-or-create the row, set
 *       {@code messagesJson = ChatMessageSerializer.messagesToJson(messages)}, {@code persist()}.</li>
 *   <li>{@code deleteMessages}: {@code ChatMemoryEntity.deleteById(key)}.</li>
 * </ul>
 */
@ApplicationScoped
public class PersistentChatMemoryStore implements ChatMemoryStore {

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        // TODO: load the row for this memoryId and deserialize its messagesJson.
        throw new UnsupportedOperationException("TODO: Exercise 10 — read from ChatMemoryEntity");
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        // TODO: upsert the row for this memoryId, serializing messages to JSON.
        throw new UnsupportedOperationException("TODO: Exercise 10 — write to ChatMemoryEntity");
    }

    @Override
    public void deleteMessages(Object memoryId) {
        // TODO: delete the row for this memoryId.
        throw new UnsupportedOperationException("TODO: Exercise 10 — delete ChatMemoryEntity");
    }
}

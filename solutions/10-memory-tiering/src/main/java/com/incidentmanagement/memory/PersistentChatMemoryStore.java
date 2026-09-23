package com.incidentmanagement.memory;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

/**
 * Tier 2 — the durable backing store for chat memory.
 *
 * <p>Providing a {@code ChatMemoryStore} CDI bean is all it takes: quarkus-langchain4j
 * uses it in place of its default in-memory store, so the {@code MessageWindowChatMemory}
 * (Tier 1) now reads and writes through PostgreSQL. Serialization of the {@link ChatMessage}
 * list is handled by LangChain4j's {@code ChatMessageSerializer}/{@code Deserializer} —
 * we only move JSON in and out of a row.
 */
@ApplicationScoped
public class PersistentChatMemoryStore implements ChatMemoryStore {

    @Override
    @Transactional
    public List<ChatMessage> getMessages(Object memoryId) {
        ChatMemoryEntity entity = ChatMemoryEntity.findById(key(memoryId));
        if (entity == null || entity.messagesJson == null) {
            return List.of();
        }
        return ChatMessageDeserializer.messagesFromJson(entity.messagesJson);
    }

    @Override
    @Transactional
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String id = key(memoryId);
        ChatMemoryEntity entity = ChatMemoryEntity.findById(id);
        if (entity == null) {
            entity = new ChatMemoryEntity();
            entity.id = id;
        }
        entity.messagesJson = ChatMessageSerializer.messagesToJson(messages);
        entity.persist();
    }

    @Override
    @Transactional
    public void deleteMessages(Object memoryId) {
        ChatMemoryEntity.deleteById(key(memoryId));
    }

    private static String key(Object memoryId) {
        return String.valueOf(memoryId);
    }
}

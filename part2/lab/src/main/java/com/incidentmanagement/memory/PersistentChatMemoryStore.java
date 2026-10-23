package com.incidentmanagement.memory;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

/**
 * Explicitly scoped conversation storage. The CDI bean does not implement ChatMemoryStore:
 * otherwise Quarkus would also attach it to unrelated stateless agents and parallel voters.
 * Only IncidentAssistant receives the adapter below.
 */
@ApplicationScoped
public class PersistentChatMemoryStore {

    @Transactional
    public List<ChatMessage> getMessages(Object memoryId) {
        ChatMemoryEntity entity = ChatMemoryEntity.findById(key(memoryId));
        if (entity == null || entity.messagesJson == null) {
            return List.of();
        }
        return ChatMessageDeserializer.messagesFromJson(entity.messagesJson);
    }

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

    @Transactional
    public void deleteMessages(Object memoryId) {
        ChatMemoryEntity.deleteById(key(memoryId));
    }

    public ChatMemoryStore asChatMemoryStore() {
        // Calls through this CDI proxy retain the transactional method interceptors.
        PersistentChatMemoryStore delegate = io.quarkus.arc.Arc.container()
                .instance(PersistentChatMemoryStore.class).get();
        return new ChatMemoryStore() {
            @Override public List<ChatMessage> getMessages(Object id) { return delegate.getMessages(id); }
            @Override public void updateMessages(Object id, List<ChatMessage> messages) { delegate.updateMessages(id, messages); }
            @Override public void deleteMessages(Object id) { delegate.deleteMessages(id); }
        };
    }

    private static String key(Object memoryId) {
        return String.valueOf(memoryId);
    }
}

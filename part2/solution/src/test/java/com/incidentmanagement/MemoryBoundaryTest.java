package com.incidentmanagement;

import com.incidentmanagement.agentic.agents.IncidentAssistant;
import com.incidentmanagement.memory.PersistentChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest @QuarkusTestResource(ScriptedModel.class)
class MemoryBoundaryTest {
    @org.junit.jupiter.api.BeforeEach
    void resetScriptedModel() { ScriptedModel.replies.clear(); ScriptedModel.requests.clear(); }

    @Inject PersistentChatMemoryStore store;
    @Inject IncidentAssistant assistant;
    @Test void evictedMessagesLeaveTheDatabaseAndConversationsStayIsolated() {
        String key="eviction-test";
        try {
            var memory=MessageWindowChatMemory.builder().id(key).maxMessages(3).chatMemoryStore(store.asChatMemoryStore()).build();
            memory.add(SystemMessage.from("Use facts"));
            memory.add(UserMessage.from("Secret old symptom"));
            memory.add(AiMessage.from("Acknowledged"));
            memory.add(UserMessage.from("New detail"));
            assertEquals(3, store.getMessages(key).size());
            assertFalse(store.getMessages(key).toString().contains("Secret old symptom"));
            assertTrue(store.getMessages("other-incident").isEmpty());
            var reloaded=MessageWindowChatMemory.builder().id(key).maxMessages(3).chatMemoryStore(store.asChatMemoryStore()).build();
            assertEquals(memory.messages(), reloaded.messages());
        } finally { store.deleteMessages(key); }
    }
    @Test void declarativeAgentUsesTheStoredWindowOnItsNextCall() {
        store.deleteMessages(9001); ScriptedModel.requests.clear();
        ScriptedModel.replies.add("Acknowledged OOMKilled");
        ScriptedModel.replies.add("You reported OOMKilled");
        try {
            assistant.chat(9001,"OOMKilled every five minutes");
            assistant.chat(9001,"What symptom did I report?");
            assertEquals(5,store.getMessages(9001).size());
            assertTrue(ScriptedModel.requests.getLast().contains("OOMKilled every five minutes"));
            assertTrue(store.getMessages(9002).isEmpty());
        } finally { store.deleteMessages(9001); }
    }
}

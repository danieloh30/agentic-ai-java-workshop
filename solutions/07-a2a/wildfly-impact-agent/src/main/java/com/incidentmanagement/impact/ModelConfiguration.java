package com.incidentmanagement.impact;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.time.Duration;

@ApplicationScoped
public class ModelConfiguration {
    @Produces
    @ApplicationScoped
    ChatModel model() {
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("Export OPENAI_API_KEY before starting WildFly");
        return OpenAiChatModel.builder()
                .apiKey(key)
                .baseUrl(System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1"))
                .modelName(System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o"))
                .temperature(0.0).timeout(Duration.ofSeconds(60)).maxRetries(0)
                .build();
    }
}

package com.pettimeline.ai.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Value("${app.ai.zhipu.api-key}")
    private String apiKey;

    @Value("${app.ai.zhipu.base-url}")
    private String baseUrl;

    @Value("${app.ai.zhipu.chat-model:glm-4-plus}")
    private String chatModel;

    @Value("${app.ai.zhipu.embedding-model:embedding-2}")
    private String embeddingModel;

    @Bean
    public OpenAiChatModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(chatModel)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(2)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(embeddingModel)
                .timeout(Duration.ofSeconds(30))
                .maxRetries(2)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}

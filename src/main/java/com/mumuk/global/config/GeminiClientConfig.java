package com.mumuk.global.config;

import com.mumuk.global.client.GeminiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GeminiClientConfig {

    @Value("${gemini.api.url}")
    private String baseUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }

    @Bean
    @org.springframework.context.annotation.Primary
    public GeminiClient geminiClient(WebClient geminiWebClient,
                                     @Value("${gemini.api.model}") String model,
                                     @Value("${gemini.api.model_accurate:${gemini.api.model}}") String accurateModel) {
        return new GeminiClient(geminiWebClient, model, accurateModel);
    }
}

package com.graduate.novel.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gemini.api")
@Getter
@Setter
public class GeminiConfig {
    private String key;
    private String baseUrl;
    private Model model;
    private Long timeout;
    private Integer maxRetries;
    private Long retryDelayMs;
    private Long rateLimitDelayMs;

    @Getter
    @Setter
    public static class Model {
        private String text;
        private String embedding;
    }
}


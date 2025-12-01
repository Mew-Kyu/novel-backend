package com.graduate.novel.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate geminiRestTemplate(GeminiConfig geminiConfig) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) geminiConfig.getTimeout().longValue());
        factory.setReadTimeout((int) geminiConfig.getTimeout().longValue());

        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }
}


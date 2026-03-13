package com.graduate.novel.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate geminiRestTemplate(GeminiConfig geminiConfig) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // Set connection and read timeouts
        factory.setConnectTimeout((int) geminiConfig.getTimeout().longValue());
        factory.setReadTimeout((int) geminiConfig.getTimeout().longValue());

        // Configure proxy if provided
        if (geminiConfig.getProxy() != null && geminiConfig.getProxy().getHost() != null && geminiConfig.getProxy().getPort() != null) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(geminiConfig.getProxy().getHost(), geminiConfig.getProxy().getPort()));
            factory.setProxy(proxy);
        }

        return new RestTemplate(factory);
    }
}

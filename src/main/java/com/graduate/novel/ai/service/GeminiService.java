package com.graduate.novel.ai.service;

import com.graduate.novel.ai.config.GeminiConfig;
import com.graduate.novel.ai.dto.*;
import com.graduate.novel.common.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final GeminiConfig geminiConfig;
    private final RestTemplate geminiRestTemplate;

    /**
     * Generate text content using Gemini API
     */
    public String generateContent(String prompt) {
        return generateContent(prompt, null);
    }

    /**
     * Generate text content with custom configuration
     */
    public String generateContent(String prompt, GeminiRequest.GenerationConfig config) {
        int retries = 0;
        int maxRetries = geminiConfig.getMaxRetries();
        long retryDelay = 2000; // Start with 2 seconds

        while (retries <= maxRetries) {
            try {
                String url = String.format("%s/models/%s:generateContent?key=%s",
                        geminiConfig.getBaseUrl(),
                        geminiConfig.getModel().getText(),
                        geminiConfig.getKey());

                GeminiRequest request = buildRequest(prompt, config);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Accept-Charset", "UTF-8");

                HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

                log.debug("Sending request to Gemini API (attempt {}/{})", retries + 1, maxRetries + 1);

                ResponseEntity<GeminiResponse> response = geminiRestTemplate.postForEntity(
                        url, entity, GeminiResponse.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    GeminiResponse geminiResponse = response.getBody();
                    if (geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()) {
                        String generatedText = geminiResponse.getCandidates().get(0)
                                .getContent().getParts().get(0).getText();
                        log.debug("Successfully generated content from Gemini");
                        return generatedText;
                    }
                }

                log.error("Failed to generate content: No valid response from Gemini");
                return null;

            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                retries++;
                if (retries > maxRetries) {
                    long retryAfterSeconds = extractRetryAfterSeconds(e.getResponseBodyAsString());
                    log.error("Max retries exceeded for rate limit. Please wait and try again later.");

                    String errorMessage = "Đã vượt quá giới hạn số lần gọi API Gemini. " +
                            "Vui lòng thử lại sau " + (retryAfterSeconds > 0 ? retryAfterSeconds + " giây" : "ít phút") + ". " +
                            "Bạn có thể kiểm tra quota tại: https://ai.dev/usage?tab=rate-limit";

                    throw new RateLimitExceededException(errorMessage, retryAfterSeconds, e);
                }

                log.warn("Rate limit hit (429). Retrying in {} ms... (attempt {}/{})",
                        retryDelay, retries, maxRetries);

                try {
                    Thread.sleep(retryDelay);
                    retryDelay *= 2; // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting to retry", ie);
                }

            } catch (org.springframework.web.client.HttpClientErrorException e) {
                log.error("HTTP Client Error ({}): {}", e.getStatusCode(), e.getMessage());
                log.error("Response body: {}", e.getResponseBodyAsString());
                throw new RuntimeException("Failed to generate content: " + e.getMessage(), e);

            } catch (Exception e) {
                log.error("Error calling Gemini API: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to generate content from Gemini API", e);
            }
        }

        throw new RuntimeException("Failed to generate content after " + maxRetries + " retries");
    }

    /**
     * Generate embedding vector for text
     */
    public float[] generateEmbedding(String text) {
        int retries = 0;
        int maxRetries = geminiConfig.getMaxRetries();
        long retryDelay = 2000; // Start with 2 seconds

        while (retries <= maxRetries) {
            try {
                String url = String.format("%s/models/%s:embedContent?key=%s",
                        geminiConfig.getBaseUrl(),
                        geminiConfig.getModel().getEmbedding(),
                        geminiConfig.getKey());

                GeminiEmbeddingRequest request = GeminiEmbeddingRequest.builder()
                        .content(GeminiEmbeddingRequest.Content.builder()
                                .parts(List.of(GeminiEmbeddingRequest.Part.builder()
                                        .text(text)
                                        .build()))
                                .build())
                        .build();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Accept-Charset", "UTF-8");

                HttpEntity<GeminiEmbeddingRequest> entity = new HttpEntity<>(request, headers);

                log.debug("Generating embedding from Gemini API (attempt {}/{})", retries + 1, maxRetries + 1);

                ResponseEntity<GeminiEmbeddingResponse> response = geminiRestTemplate.postForEntity(
                        url, entity, GeminiEmbeddingResponse.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    float[] embedding = response.getBody().getEmbedding().getValues();
                    log.debug("Successfully generated embedding with {} dimensions", embedding.length);
                    return embedding;
                }

                log.error("Failed to generate embedding: No valid response from Gemini");
                return null;

            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                retries++;
                if (retries > maxRetries) {
                    long retryAfterSeconds = extractRetryAfterSeconds(e.getResponseBodyAsString());
                    log.error("Max retries exceeded for rate limit. Please wait and try again later.");

                    String errorMessage = "Đã vượt quá giới hạn số lần gọi API Gemini. " +
                            "Vui lòng thử lại sau " + (retryAfterSeconds > 0 ? retryAfterSeconds + " giây" : "ít phút") + ". " +
                            "Bạn có thể kiểm tra quota tại: https://ai.dev/usage?tab=rate-limit";

                    throw new RateLimitExceededException(errorMessage, retryAfterSeconds, e);
                }

                log.warn("Rate limit hit (429). Retrying in {} ms... (attempt {}/{})",
                        retryDelay, retries, maxRetries);

                try {
                    Thread.sleep(retryDelay);
                    retryDelay *= 2; // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting to retry", ie);
                }

            } catch (org.springframework.web.client.HttpClientErrorException e) {
                log.error("HTTP Client Error ({}): {}", e.getStatusCode(), e.getMessage());
                log.error("Response body: {}", e.getResponseBodyAsString());
                throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);

            } catch (Exception e) {
                log.error("Error generating embedding from Gemini API: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to generate embedding from Gemini API", e);
            }
        }

        throw new RuntimeException("Failed to generate embedding after " + maxRetries + " retries");
    }

    /**
     * Build a Gemini request with default or custom configuration
     */
    private GeminiRequest buildRequest(String prompt, GeminiRequest.GenerationConfig config) {
        if (config == null) {
            config = GeminiRequest.GenerationConfig.builder()
                    .temperature(0.7)
                    .maxOutputTokens(8192)
                    .topP(0.95)
                    .topK(40)
                    .build();
        }

        return GeminiRequest.builder()
                .contents(List.of(
                        GeminiRequest.Content.builder()
                                .parts(List.of(
                                        GeminiRequest.Part.builder()
                                                .text(prompt)
                                                .build()
                                ))
                                .build()
                ))
                .generationConfig(config)
                .build();
    }

    /**
     * Extract retry-after seconds from Gemini API error response
     * Example: "Please retry in 52.013683842s."
     */
    private long extractRetryAfterSeconds(String errorBody) {
        try {
            // Pattern to match "retry in XX.XXs" or "retry in XXs"
            Pattern pattern = Pattern.compile("retry in ([0-9.]+)s", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(errorBody);

            if (matcher.find()) {
                String secondsStr = matcher.group(1);
                double seconds = Double.parseDouble(secondsStr);
                return (long) Math.ceil(seconds); // Round up to nearest second
            }
        } catch (Exception e) {
            log.warn("Failed to extract retry-after seconds from error response: {}", e.getMessage());
        }
        return 0;
    }
}


package com.graduate.novel.ai.service;

import com.graduate.novel.ai.dto.GeminiRequest;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService {

    private final GeminiService geminiService;
    private final StoryRepository storyRepository;

    /**
     * Translate Japanese text to Vietnamese
     */
    public String translateToVietnamese(String japaneseText) {
        if (japaneseText == null || japaneseText.trim().isEmpty()) {
            log.warn("Empty text provided for translation");
            return "";
        }

        log.info("Translating text from Japanese to Vietnamese (length: {} chars)", japaneseText.length());

        String prompt = buildTranslationPrompt(japaneseText);

        // Use specific configuration for translation
        GeminiRequest.GenerationConfig config = GeminiRequest.GenerationConfig.builder()
                .temperature(0.3)  // Lower temperature for more consistent translations
                .maxOutputTokens(8192)
                .topP(0.95)
                .topK(40)
                .build();

        int maxRetries = 3;
        long retryDelay = 2000;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String translation = geminiService.generateContent(prompt, config);
                log.info("Translation completed successfully");
                return translation != null ? translation.trim() : "";

            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                if (attempt < maxRetries) {
                    log.warn("Rate limit hit during translation. Retrying in {} ms... (attempt {}/{})",
                            retryDelay, attempt + 1, maxRetries);
                    try {
                        Thread.sleep(retryDelay);
                        retryDelay *= 2; // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                } else {
                    log.error("Translation failed after {} attempts due to rate limiting", maxRetries + 1);
                    throw new RuntimeException("Rate limit exceeded. Please try again later.", e);
                }

            } catch (Exception e) {
                log.error("Translation failed: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to translate text", e);
            }
        }

        throw new RuntimeException("Failed to translate after " + maxRetries + " retries");
    }

    /**
     * Translate English text to Vietnamese
     */
    public String translateEnglishToVietnamese(String englishText) {
        if (englishText == null || englishText.trim().isEmpty()) {
            log.warn("Empty text provided for translation");
            return "";
        }

        log.info("Translating text from English to Vietnamese (length: {} chars)", englishText.length());

        String prompt = buildEnglishTranslationPrompt(englishText);

        GeminiRequest.GenerationConfig config = GeminiRequest.GenerationConfig.builder()
                .temperature(0.3)
                .maxOutputTokens(8192)
                .topP(0.95)
                .topK(40)
                .build();

        try {
            String translation = geminiService.generateContent(prompt, config);
            log.info("Translation completed successfully");
            return translation != null ? translation.trim() : "";
        } catch (Exception e) {
            log.error("Translation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to translate text", e);
        }
    }

    /**
     * Build translation prompt for Japanese to Vietnamese
     */
    private String buildTranslationPrompt(String japaneseText) {
        return String.format(
                "Bạn là một dịch giả chuyên nghiệp. Hãy dịch văn bản tiếng Nhật sau sang tiếng Việt. " +
                        "Giữ nguyên định dạng, ngắt dòng và phong cách văn bản gốc. " +
                        "Chỉ trả về bản dịch tiếng Việt, không thêm giải thích hay chú thích.\n\n" +
                        "Văn bản tiếng Nhật:\n%s",
                japaneseText
        );
    }

    /**
     * Build translation prompt for English to Vietnamese
     */
    private String buildEnglishTranslationPrompt(String englishText) {
        return String.format(
                "Bạn là một dịch giả chuyên nghiệp. Hãy dịch văn bản tiếng Anh sau sang tiếng Việt. " +
                        "Giữ nguyên định dạng, ngắt dòng và phong cách văn bản gốc. " +
                        "Chỉ trả về bản dịch tiếng Việt, không thêm giải thích hay chú thích.\n\n" +
                        "Văn bản tiếng Anh:\n%s",
                englishText
        );
    }

    /**
     * Detect and translate automatically
     */
    public String autoTranslate(String text, String sourceLanguage) {
        if ("ja".equalsIgnoreCase(sourceLanguage) || "japanese".equalsIgnoreCase(sourceLanguage)) {
            return translateToVietnamese(text);
        } else if ("en".equalsIgnoreCase(sourceLanguage) || "english".equalsIgnoreCase(sourceLanguage)) {
            return translateEnglishToVietnamese(text);
        } else {
            // Default to Japanese
            return translateToVietnamese(text);
        }
    }

    /**
     * Translate story title and/or description from raw (Japanese) to Vietnamese
     */
    @Transactional
    public Story translateStory(Long storyId, boolean translateTitle, boolean translateDescription) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with id: " + storyId));

        log.info("Translating story ID: {} (title: {}, description: {})",
                storyId, translateTitle, translateDescription);

        boolean updated = false;

        // Translate title if requested and raw title exists
        if (translateTitle && story.getRawTitle() != null && !story.getRawTitle().trim().isEmpty()) {
            log.info("Translating title for story ID: {}", storyId);
            String translatedTitle = translateToVietnamese(story.getRawTitle());
            story.setTranslatedTitle(translatedTitle);
            // Also update the main title field for backward compatibility
            story.setTitle(translatedTitle);
            updated = true;
            log.info("Title translated successfully");
        }

        // Translate description if requested and raw description exists
        if (translateDescription && story.getRawDescription() != null && !story.getRawDescription().trim().isEmpty()) {
            log.info("Translating description for story ID: {}", storyId);
            String translatedDescription = translateToVietnamese(story.getRawDescription());
            story.setTranslatedDescription(translatedDescription);
            // Also update the main description field for backward compatibility
            story.setDescription(translatedDescription);
            updated = true;
            log.info("Description translated successfully");
        }

        if (updated) {
            story = storyRepository.save(story);
            log.info("Story translations saved successfully");
        } else {
            log.warn("No translation performed for story ID: {}", storyId);
        }

        return story;
    }
}


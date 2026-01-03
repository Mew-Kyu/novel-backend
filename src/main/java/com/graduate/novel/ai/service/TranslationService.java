package com.graduate.novel.ai.service;

import com.graduate.novel.ai.dto.GeminiRequest;
import com.graduate.novel.common.exception.BadRequestException;
import com.graduate.novel.domain.crawljob.CrawlJobService;
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
    private final CrawlJobService crawlJobService;

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
     * Romanize Japanese text (convert Kanji/Hiragana to Romaji)
     * Used for author names to keep them readable but not translated
     */
    public String romanizeJapanese(String japaneseText) {
        if (japaneseText == null || japaneseText.trim().isEmpty()) {
            log.warn("Empty text provided for romanization");
            return "";
        }

        log.info("Romanizing Japanese text to Romaji (length: {} chars)", japaneseText.length());

        String prompt = buildRomanizationPrompt(japaneseText);

        GeminiRequest.GenerationConfig config = GeminiRequest.GenerationConfig.builder()
                .temperature(0.1)  // Very low temperature for consistent romanization
                .maxOutputTokens(512)
                .topP(0.95)
                .topK(40)
                .build();

        try {
            String romanized = geminiService.generateContent(prompt, config);
            log.info("Romanization completed successfully");
            return romanized != null ? romanized.trim() : "";
        } catch (Exception e) {
            log.error("Romanization failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to romanize text", e);
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
     * Build romanization prompt for Japanese to Romaji
     */
    private String buildRomanizationPrompt(String japaneseText) {
        return String.format(
                "Convert the following Japanese text to Romaji (Roman alphabet). " +
                        "Use standard Hepburn romanization. " +
                        "Do not translate the meaning, only convert the pronunciation to Roman letters. " +
                        "Return only the romanized text without any explanations or notes.\n\n" +
                        "Japanese text:\n%s\n\n" +
                        "Romaji:",
                japaneseText
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

        // Check if there's already an active translation job for this story
        boolean hasActiveTranslationJob = crawlJobService.hasActiveJobForStory(storyId, "STORY_TRANSLATE");
        if (hasActiveTranslationJob) {
            throw new BadRequestException(
                String.format("Story %d is already being translated. Please wait for the current job to complete.",
                    storyId)
            );
        }

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

        // Romanize author name if raw author name exists (convert to Romaji, not translate)
        if (story.getRawAuthorName() != null && !story.getRawAuthorName().trim().isEmpty()) {
            log.info("Romanizing author name for story ID: {}", storyId);
            String romanizedAuthorName = romanizeJapanese(story.getRawAuthorName());
            story.setTranslatedAuthorName(romanizedAuthorName);
            // Also update the main authorName field for backward compatibility
            story.setAuthorName(romanizedAuthorName);
            updated = true;
            log.info("Author name romanized successfully: {} → {}", story.getRawAuthorName(), romanizedAuthorName);
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


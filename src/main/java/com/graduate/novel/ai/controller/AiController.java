package com.graduate.novel.ai.controller;

import com.graduate.novel.ai.dto.TranslationRequest;
import com.graduate.novel.ai.dto.TranslationResponse;
import com.graduate.novel.ai.dto.SemanticSearchRequest;
import com.graduate.novel.ai.dto.SemanticSearchResponse;
import com.graduate.novel.ai.service.TranslationService;
import com.graduate.novel.ai.service.SemanticSearchService;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryDto;
import com.graduate.novel.common.mapper.StoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final TranslationService translationService;
    private final SemanticSearchService semanticSearchService;
    private final StoryMapper storyMapper;

    /**
     * Translate text from Japanese to Vietnamese
     */
    @PostMapping("/translate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<TranslationResponse> translate(@RequestBody TranslationRequest request) {
        log.info("Received translation request for text of length: {}", request.getText().length());

        String translatedText = translationService.translateToVietnamese(request.getText());

        TranslationResponse response = TranslationResponse.builder()
                .originalText(request.getText())
                .translatedText(translatedText)
                .sourceLanguage("ja")
                .targetLanguage("vi")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Auto-translate with language detection
     */
    @PostMapping("/translate/auto")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<TranslationResponse> autoTranslate(@RequestBody TranslationRequest request) {
        log.info("Received auto-translation request");

        String sourceLanguage = request.getSourceLanguage() != null ? request.getSourceLanguage() : "ja";
        String translatedText = translationService.autoTranslate(request.getText(), sourceLanguage);

        TranslationResponse response = TranslationResponse.builder()
                .originalText(request.getText())
                .translatedText(translatedText)
                .sourceLanguage(sourceLanguage)
                .targetLanguage("vi")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Generate embedding for a specific story
     */
    @PostMapping("/embeddings/story/{storyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<Map<String, String>> generateStoryEmbedding(@PathVariable Long storyId) {
        log.info("Generating embedding for story id: {}", storyId);

        semanticSearchService.generateStoryEmbedding(storyId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Embedding generated successfully");
        response.put("storyId", storyId.toString());

        return ResponseEntity.ok(response);
    }

    /**
     * Generate embeddings for all stories without embeddings
     */
    @PostMapping("/embeddings/generate-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<Map<String, String>> generateAllEmbeddings() {
        log.info("Starting bulk embedding generation");

        // Run in a separate thread to avoid timeout
        new Thread(() -> {
            try {
                semanticSearchService.generateAllMissingEmbeddings();
            } catch (Exception e) {
                log.error("Error in bulk embedding generation: {}", e.getMessage());
            }
        }).start();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Embedding generation started in background");

        return ResponseEntity.accepted().body(response);
    }

    /**
     * Semantic search for stories (public access)
     */
    @PostMapping("/search/semantic")
    public ResponseEntity<SemanticSearchResponse> semanticSearch(@RequestBody SemanticSearchRequest request) {
        try {
            int rawLimit = request.getLimit() != null ? request.getLimit() : 5;
            int limit = Math.max(1, Math.min(rawLimit, 50));

            // Let service handle embedding generation + similarity search in one flow.
            List<Story> stories = semanticSearchService.searchBySimilarity(request.getQuery(), limit);

            List<StoryDto> storyDtos = stories.stream()
                    .map(storyMapper::toDto)
                    .collect(Collectors.toList());

            SemanticSearchResponse response = SemanticSearchResponse.builder()
                    .query(request.getQuery())
                    .results(storyDtos)
                    .totalResults(storyDtos.size())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during semantic search: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Diagnostic endpoint to test semantic search with different queries
     * Returns detailed debug information
     */
    @PostMapping("/search/semantic/debug")
    public ResponseEntity<Map<String, Object>> semanticSearchDebug(@RequestBody SemanticSearchRequest request) {
        Map<String, Object> debugInfo = new HashMap<>();

        try {
            float[] queryEmbedding = semanticSearchService.generateEmbedding(request.getQuery());

            debugInfo.put("queryEmbedding_generated", queryEmbedding != null);
            if (queryEmbedding != null) {
                debugInfo.put("embedding_dimensions", queryEmbedding.length);
                debugInfo.put("first_5_values", new float[]{
                    queryEmbedding[0], queryEmbedding[1], queryEmbedding[2],
                    queryEmbedding[3], queryEmbedding[4]
                });

                int limit = request.getLimit() != null ? request.getLimit() : 10;
                List<Story> stories = semanticSearchService.searchBySimilarity(request.getQuery(), limit);

                debugInfo.put("stories_found", stories.size());
                debugInfo.put("search_result_ids", stories.stream().map(Story::getId).toList());
                debugInfo.put("search_result_titles", stories.stream().map(Story::getTitle).toList());

                List<StoryDto> storyDtos = stories.stream()
                        .map(storyMapper::toDto)
                        .collect(Collectors.toList());

                SemanticSearchResponse response = SemanticSearchResponse.builder()
                        .query(request.getQuery())
                        .results(storyDtos)
                        .totalResults(storyDtos.size())
                        .build();

                debugInfo.put("response", response);
                debugInfo.put("status", "SUCCESS");
            } else {
                debugInfo.put("status", "FAILED - Embedding is NULL");
            }

        } catch (Exception e) {
            log.error("Debug endpoint error: {}", e.getMessage(), e);
            debugInfo.put("status", "ERROR");
            debugInfo.put("error", e.getMessage());
            debugInfo.put("error_type", e.getClass().getSimpleName());
        }

        return ResponseEntity.ok(debugInfo);
    }

    /**
     * Refresh embedding for a story (useful after updates)
     */
    @PutMapping("/embeddings/story/{storyId}/refresh")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<Map<String, String>> refreshStoryEmbedding(@PathVariable Long storyId) {
        log.info("Refreshing embedding for story id: {}", storyId);

        semanticSearchService.refreshStoryEmbedding(storyId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Embedding refreshed successfully");
        response.put("storyId", storyId.toString());

        return ResponseEntity.ok(response);
    }

    /**
     * Health check for AI services
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "AI Services (Translation & Semantic Search)");
        return ResponseEntity.ok(response);
    }
}

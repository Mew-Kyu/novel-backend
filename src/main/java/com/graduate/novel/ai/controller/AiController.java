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
     * Semantic search for stories
     */
    @PostMapping("/search/semantic")
    public ResponseEntity<SemanticSearchResponse> semanticSearch(@RequestBody SemanticSearchRequest request) {
        log.info("Performing semantic search for query: {}", request.getQuery());

        int limit = request.getLimit() != null ? request.getLimit() : 10;
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
    }

    /**
     * Refresh embedding for a story (useful after updates)
     */
    @PutMapping("/embeddings/story/{storyId}/refresh")
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


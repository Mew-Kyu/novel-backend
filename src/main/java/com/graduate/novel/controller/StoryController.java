package com.graduate.novel.controller;

import com.graduate.novel.ai.dto.TranslateStoryRequest;
import com.graduate.novel.ai.dto.TranslateStoryResponse;
import com.graduate.novel.ai.service.TranslationService;
import com.graduate.novel.domain.story.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
@Slf4j
public class StoryController {

    private final StoryService storyService;
    private final TranslationService translationService;

    // ========== Homepage Endpoints ==========

    /**
     * Get stories with full metadata (for homepage)
     */
    @GetMapping("/with-metadata")
    public ResponseEntity<Page<StoryDetailDto>> getStoriesWithMetadata(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) String genre,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<StoryDetailDto> stories;
        if (genreId != null) {
            stories = storyService.getStoriesByGenreWithMetadata(genreId, pageable);
        } else if (genre != null && !genre.isEmpty()) {
            stories = storyService.getStoriesByGenreNameWithMetadata(genre, pageable);
        } else {
            stories = storyService.getStoriesWithMetadata(keyword, pageable);
        }
        return ResponseEntity.ok(stories);
    }

    /**
     * Get featured stories
     */
    @GetMapping("/featured")
    public ResponseEntity<java.util.List<StoryDetailDto>> getFeaturedStories(
            @RequestParam(defaultValue = "5") int limit
    ) {
        java.util.List<StoryDetailDto> stories = storyService.getFeaturedStories(limit);
        return ResponseEntity.ok(stories);
    }

    /**
     * Get trending stories
     */
    @GetMapping("/trending")
    public ResponseEntity<java.util.List<StoryDetailDto>> getTrendingStories(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "7") int days
    ) {
        java.util.List<StoryDetailDto> stories = storyService.getTrendingStories(limit, days);
        return ResponseEntity.ok(stories);
    }

    /**
     * Increment view count
     */
    @PostMapping("/{id}/view")
    public ResponseEntity<Void> incrementViewCount(@PathVariable Long id) {
        storyService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Set featured status (Admin only)
     */
    @PatchMapping("/{id}/featured")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StoryDto> setFeatured(
            @PathVariable Long id,
            @RequestParam boolean featured
    ) {
        StoryDto story = storyService.setFeatured(id, featured);
        return ResponseEntity.ok(story);
    }

    // ========== Existing Endpoints ==========

    @GetMapping
    public ResponseEntity<Page<StoryDto>> getStories(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) String genre,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<StoryDto> stories;
        if (genreId != null) {
            stories = storyService.getStoriesByGenre(genreId, pageable);
        } else if (genre != null && !genre.isEmpty()) {
            stories = storyService.getStoriesByGenreName(genre, pageable);
        } else {
            stories = storyService.getStories(keyword, pageable);
        }
        return ResponseEntity.ok(stories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoryDto> getStoryById(@PathVariable Long id) {
        StoryDto story = storyService.getStoryById(id);
        return ResponseEntity.ok(story);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<StoryDto> createStory(@Valid @RequestBody CreateStoryRequest request) {
        StoryDto story = storyService.createStory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(story);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<StoryDto> updateStory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStoryRequest request
    ) {
        StoryDto story = storyService.updateStory(id, request);
        return ResponseEntity.ok(story);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<Void> deleteStory(@PathVariable Long id) {
        storyService.deleteStory(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Translate story title and/or description
     */
    @PostMapping("/translate/story")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<TranslateStoryResponse> translateStory(@RequestBody TranslateStoryRequest request) {
        log.info("Translating story id: {} (title: {}, description: {})",
                request.getStoryId(), request.getTranslateTitle(), request.getTranslateDescription());

        Story story = translationService.translateStory(
                request.getStoryId(),
                request.getTranslateTitle(),
                request.getTranslateDescription()
        );

        TranslateStoryResponse response = TranslateStoryResponse.builder()
                .storyId(story.getId())
                .originalTitle(story.getRawTitle())
                .translatedTitle(story.getTranslatedTitle())
                .originalAuthorName(story.getRawAuthorName())
                .translatedAuthorName(story.getTranslatedAuthorName())
                .originalDescription(story.getRawDescription())
                .translatedDescription(story.getTranslatedDescription())
                .message("Story translated successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Translate story title and description by story ID (translate both by default)
     */
    @PostMapping("/translate/story/{storyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<TranslateStoryResponse> translateStoryById(@PathVariable Long storyId) {
        log.info("Translating story id: {} (both title and description)", storyId);

        Story story = translationService.translateStory(storyId, true, true);

        TranslateStoryResponse response = TranslateStoryResponse.builder()
                .storyId(story.getId())
                .originalTitle(story.getRawTitle())
                .translatedTitle(story.getTranslatedTitle())
                .originalAuthorName(story.getRawAuthorName())
                .translatedAuthorName(story.getTranslatedAuthorName())
                .originalDescription(story.getRawDescription())
                .translatedDescription(story.getTranslatedDescription())
                .message("Story translated successfully")
                .build();

        return ResponseEntity.ok(response);
    }

}

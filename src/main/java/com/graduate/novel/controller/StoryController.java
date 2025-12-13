package com.graduate.novel.controller;

import com.graduate.novel.ai.dto.TranslateStoryRequest;
import com.graduate.novel.ai.dto.TranslateStoryResponse;
import com.graduate.novel.ai.service.TranslationService;
import com.graduate.novel.domain.story.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Story Management", description = "APIs for managing stories and their genres")
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
    @Operation(
            summary = "Create a new story",
            description = "Create a new story with optional multiple genres. Requires ADMIN or MODERATOR role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Story created successfully",
                    content = @Content(schema = @Schema(implementation = StoryDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or MODERATOR role"),
            @ApiResponse(responseCode = "404", description = "Genre not found")
    })
    public ResponseEntity<StoryDto> createStory(@Valid @RequestBody CreateStoryRequest request) {
        StoryDto story = storyService.createStory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(story);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @Operation(
            summary = "Update a story",
            description = "Update story details and optionally replace all genres. Requires ADMIN or MODERATOR role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Story updated successfully",
                    content = @Content(schema = @Schema(implementation = StoryDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or MODERATOR role"),
            @ApiResponse(responseCode = "404", description = "Story or genre not found")
    })
    public ResponseEntity<StoryDto> updateStory(
            @Parameter(description = "Story ID", required = true) @PathVariable Long id,
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

    // ========== Genre Management Endpoints ==========

    @PostMapping("/{storyId}/genres/{genreId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @Operation(
            summary = "Add a genre to a story",
            description = "Add a single genre to an existing story without affecting other genres. Requires ADMIN or MODERATOR role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Genre added successfully",
                    content = @Content(schema = @Schema(implementation = StoryDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or MODERATOR role"),
            @ApiResponse(responseCode = "404", description = "Story or genre not found")
    })
    public ResponseEntity<StoryDto> addGenreToStory(
            @Parameter(description = "Story ID", required = true) @PathVariable Long storyId,
            @Parameter(description = "Genre ID to add", required = true) @PathVariable Long genreId
    ) {
        StoryDto story = storyService.addGenreToStory(storyId, genreId);
        return ResponseEntity.ok(story);
    }

    @DeleteMapping("/{storyId}/genres/{genreId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @Operation(
            summary = "Remove a genre from a story",
            description = "Remove a single genre from a story without affecting other genres. Requires ADMIN or MODERATOR role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Genre removed successfully",
                    content = @Content(schema = @Schema(implementation = StoryDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or MODERATOR role"),
            @ApiResponse(responseCode = "404", description = "Story or genre not found")
    })
    public ResponseEntity<StoryDto> removeGenreFromStory(
            @Parameter(description = "Story ID", required = true) @PathVariable Long storyId,
            @Parameter(description = "Genre ID to remove", required = true) @PathVariable Long genreId
    ) {
        StoryDto story = storyService.removeGenreFromStory(storyId, genreId);
        return ResponseEntity.ok(story);
    }

    @PutMapping("/{storyId}/genres")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @Operation(
            summary = "Set all genres for a story",
            description = "Replace all existing genres with a new set of genres. Send an empty array to remove all genres. Requires ADMIN or MODERATOR role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Genres updated successfully",
                    content = @Content(schema = @Schema(implementation = StoryDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or MODERATOR role"),
            @ApiResponse(responseCode = "404", description = "Story or genre not found")
    })
    public ResponseEntity<StoryDto> setGenresForStory(
            @Parameter(description = "Story ID", required = true) @PathVariable Long storyId,
            @Parameter(description = "Array of genre IDs", required = true,
                    schema = @Schema(type = "array", example = "[1, 2, 3]"))
            @RequestBody java.util.Set<Long> genreIds
    ) {
        StoryDto story = storyService.setGenresForStory(storyId, genreIds);
        return ResponseEntity.ok(story);
    }

    // ========== Translation Endpoints ==========

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

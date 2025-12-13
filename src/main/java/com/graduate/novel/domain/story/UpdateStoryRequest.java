package com.graduate.novel.domain.story;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Request to update an existing story")
public record UpdateStoryRequest(
    @Schema(description = "Story title", example = "Updated Title")
    String title,

    @Schema(description = "Author name", example = "Jane Smith")
    String authorName,

    @Schema(description = "Story description", example = "Updated description")
    String description,

    @Schema(description = "Cover image URL", example = "https://example.com/new-cover.jpg")
    String coverImageUrl,

    @Schema(description = "Source URL", example = "https://example.com/story")
    String sourceUrl,

    @Size(max = 255, message = "Source site must not exceed 255 characters")
    @Schema(description = "Source site name", example = "syosetu", maxLength = 255)
    String sourceSite,

    @Schema(description = "Array of genre IDs to replace all existing genres. Send empty array to remove all genres.",
            example = "[1, 4, 5]")
    Set<Long> genreIds
) {}

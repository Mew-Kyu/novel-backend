package com.graduate.novel.domain.story;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.graduate.novel.common.jackson.LongSetDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Request to create a new story")
public record CreateStoryRequest(
    @NotBlank(message = "Title is required")
    @Schema(description = "Story title", example = "My Epic Fantasy Adventure")
    String title,

    @Schema(description = "Author name", example = "John Doe")
    String authorName,

    @Schema(description = "Story description", example = "An epic story about...")
    String description,

    @Schema(description = "Cover image URL", example = "https://example.com/cover.jpg")
    String coverImageUrl,

    @Schema(description = "Source URL", example = "https://example.com/story")
    String sourceUrl,

    @Size(max = 255, message = "Source site must not exceed 255 characters")
    @Schema(description = "Source site name", example = "syosetu", maxLength = 255)
    String sourceSite,

    @Schema(description = "Array of genre IDs to assign to the story", example = "[1, 2, 3]")
    @JsonDeserialize(using = LongSetDeserializer.class)
    Set<Long> genreIds
) {}

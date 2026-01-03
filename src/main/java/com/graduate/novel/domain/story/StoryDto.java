package com.graduate.novel.domain.story;

import com.graduate.novel.domain.genre.GenreDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Story data transfer object")
public record StoryDto(
    @Schema(description = "Story ID", example = "1")
    Long id,

    @Schema(description = "Story title", example = "My Epic Fantasy")
    String title,

    @Schema(description = "Raw title (original language)", example = "原題")
    String rawTitle,

    @Schema(description = "Translated title", example = "My Epic Fantasy")
    String translatedTitle,

    @Schema(description = "Author name", example = "John Doe")
    String authorName,

    @Schema(description = "Raw author name (original language)", example = "著者名")
    String rawAuthorName,

    @Schema(description = "Translated author name", example = "John Doe")
    String translatedAuthorName,

    @Schema(description = "Story description")
    String description,

    @Schema(description = "Raw description (original language)")
    String rawDescription,

    @Schema(description = "Translated description")
    String translatedDescription,

    @Schema(description = "Cover image URL", example = "https://example.com/cover.jpg")
    String coverImageUrl,

    @Schema(description = "Source URL", example = "https://example.com/story")
    String sourceUrl,

    @Schema(description = "Source site name", example = "syosetu")
    String sourceSite,

    @Schema(description = "Creation timestamp", example = "2025-12-13T10:00:00")
    LocalDateTime createdAt,

    @Schema(description = "Last update timestamp", example = "2025-12-13T15:30:00")
    LocalDateTime updatedAt,

    @Schema(description = "ID of user who created this story", example = "1")
    Long createdBy,

    @Schema(description = "ID of user who last modified this story", example = "2")
    Long lastModifiedBy,

    @Schema(description = "Story publication status", example = "PUBLISHED")
    StoryStatus status,

    @Schema(description = "Whether this story is featured on homepage", example = "true")
    Boolean featured,

    @Schema(description = "List of genres associated with this story")
    List<GenreDto> genres
) {}

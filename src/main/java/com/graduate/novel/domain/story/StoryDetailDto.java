package com.graduate.novel.domain.story;

import com.graduate.novel.domain.genre.GenreDto;
import java.time.LocalDateTime;
import java.util.List;

public record StoryDetailDto(
    Long id,
    String title,
    String rawTitle,
    String translatedTitle,
    String authorName,
    String rawAuthorName,
    String translatedAuthorName,
    String description,
    String rawDescription,
    String translatedDescription,
    String coverImageUrl,
    String sourceUrl,
    String sourceSite,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,

    // Metadata
    Long viewCount,
    Boolean featured,
    Integer totalChapters,
    Double averageRating,
    Long totalRatings,
    Long totalComments,
    Long totalFavorites,

    // Relationships
    List<GenreDto> genres,
    LatestChapterInfo latestChapter
) {
    public record LatestChapterInfo(
        Long id,
        Integer chapterIndex,
        String title,
        String translatedTitle,
        LocalDateTime updatedAt
    ) {}
}


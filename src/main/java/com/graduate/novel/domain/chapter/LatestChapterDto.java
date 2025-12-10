package com.graduate.novel.domain.chapter;

import java.time.LocalDateTime;

public record LatestChapterDto(
    Long id,
    Long storyId,
    String storyTitle,
    String storyTranslatedTitle,
    Integer chapterIndex,
    String title,
    String translatedTitle,
    LocalDateTime updatedAt
) {}


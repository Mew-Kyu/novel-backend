package com.graduate.novel.domain.chapter;

import java.time.LocalDateTime;

public record ChapterDto(
    Long id,
    Long storyId,
    Integer chapterIndex,
    String title,
    String rawTitle,
    String translatedTitle,
    String rawContent,
    String crawlStatus,
    LocalDateTime crawlTime,
    String translatedContent,
    String translateStatus,
    LocalDateTime translateTime,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long createdBy,
    Long lastModifiedBy
) {}

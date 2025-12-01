package com.graduate.novel.domain.story;

import java.time.LocalDateTime;

public record StoryDto(
    Long id,
    String title,
    String rawTitle,
    String translatedTitle,
    String authorName,
    String description,
    String rawDescription,
    String translatedDescription,
    String coverImageUrl,
    String sourceUrl,
    String sourceSite,
    LocalDateTime createdAt
) {}

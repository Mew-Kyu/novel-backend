package com.graduate.novel.domain.genre;

import java.time.LocalDateTime;

public record GenreDetailDto(
    Long id,
    String name,
    String description,
    Long storyCount,
    LocalDateTime createdAt
) {}


package com.graduate.novel.domain.genre;

import java.time.LocalDateTime;

public record GenreDto(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt
) {}


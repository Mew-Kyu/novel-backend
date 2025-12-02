package com.graduate.novel.domain.rating;

import java.time.LocalDateTime;

public record RatingDto(
        Long id,
        Long userId,
        String userName,
        Long storyId,
        Integer rating,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}


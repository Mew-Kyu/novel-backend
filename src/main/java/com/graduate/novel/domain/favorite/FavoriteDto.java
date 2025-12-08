package com.graduate.novel.domain.favorite;

import com.graduate.novel.domain.story.StoryDto;
import java.time.LocalDateTime;

public record FavoriteDto(
    Long id,
    Long userId,
    StoryDto story,
    LocalDateTime createdAt
) {}


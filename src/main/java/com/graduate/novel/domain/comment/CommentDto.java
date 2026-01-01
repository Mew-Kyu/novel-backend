package com.graduate.novel.domain.comment;

import java.time.LocalDateTime;

public record CommentDto(
        Long id,
        Long userId,
        String userName,
        String userAvatarUrl,
        Long storyId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}


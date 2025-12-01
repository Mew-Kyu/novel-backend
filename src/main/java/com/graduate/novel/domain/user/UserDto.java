package com.graduate.novel.domain.user;

import java.time.LocalDateTime;

public record UserDto(
    Long id,
    String email,
    String displayName,
    LocalDateTime createdAt
) {}

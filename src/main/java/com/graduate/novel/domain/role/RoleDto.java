package com.graduate.novel.domain.role;

import java.time.LocalDateTime;

public record RoleDto(
    Long id,
    String name,
    String description,
    LocalDateTime createdAt
) {}


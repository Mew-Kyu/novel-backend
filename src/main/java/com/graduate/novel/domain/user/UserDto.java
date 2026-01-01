package com.graduate.novel.domain.user;

import com.graduate.novel.domain.role.RoleDto;

import java.time.LocalDateTime;

public record UserDto(
    Long id,
    String email,
    String displayName,
    String avatarUrl,
    LocalDateTime createdAt,
    Boolean active,
    RoleDto role
) {}

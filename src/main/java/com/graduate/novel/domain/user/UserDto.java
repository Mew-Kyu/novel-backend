package com.graduate.novel.domain.user;

import com.graduate.novel.domain.role.RoleDto;

import java.time.LocalDateTime;
import java.util.Set;

public record UserDto(
    Long id,
    String email,
    String displayName,
    LocalDateTime createdAt,
    Boolean active,
    Set<RoleDto> roles
) {}

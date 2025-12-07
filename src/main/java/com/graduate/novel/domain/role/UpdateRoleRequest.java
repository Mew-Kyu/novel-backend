package com.graduate.novel.domain.role;

import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
    @Size(max = 255, message = "Description must be at most 255 characters")
    String description
) {}


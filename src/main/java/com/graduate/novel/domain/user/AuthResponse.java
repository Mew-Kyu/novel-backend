package com.graduate.novel.domain.user;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserDto user
) {}

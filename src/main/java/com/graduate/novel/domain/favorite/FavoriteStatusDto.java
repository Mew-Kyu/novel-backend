package com.graduate.novel.domain.favorite;

public record FavoriteStatusDto(
    boolean isFavorite,
    Long favoriteCount
) {}


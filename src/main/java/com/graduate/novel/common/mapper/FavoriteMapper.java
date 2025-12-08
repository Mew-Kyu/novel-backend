package com.graduate.novel.common.mapper;

import com.graduate.novel.domain.favorite.Favorite;
import com.graduate.novel.domain.favorite.FavoriteDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = StoryMapper.class)
public interface FavoriteMapper {

    @Mapping(target = "userId", source = "user.id")
    FavoriteDto toDto(Favorite favorite);
}


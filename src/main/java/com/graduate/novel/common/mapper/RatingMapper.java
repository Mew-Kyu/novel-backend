package com.graduate.novel.common.mapper;

import com.graduate.novel.domain.rating.Rating;
import com.graduate.novel.domain.rating.RatingDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RatingMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.displayName", target = "userName")
    @Mapping(source = "story.id", target = "storyId")
    RatingDto toDto(Rating rating);
}


package com.graduate.novel.common.mapper;

import com.graduate.novel.domain.story.CreateStoryRequest;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryDto;
import com.graduate.novel.domain.story.UpdateStoryRequest;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StoryMapper {

    StoryDto toDto(Story story);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "embedding", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "rawTitle", ignore = true)
    @Mapping(target = "translatedTitle", ignore = true)
    @Mapping(target = "rawDescription", ignore = true)
    @Mapping(target = "translatedDescription", ignore = true)
    @Mapping(target = "genres", ignore = true)
    Story toEntity(CreateStoryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "embedding", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "rawTitle", ignore = true)
    @Mapping(target = "translatedTitle", ignore = true)
    @Mapping(target = "rawDescription", ignore = true)
    @Mapping(target = "translatedDescription", ignore = true)
    @Mapping(target = "genres", ignore = true)
    void updateEntity(UpdateStoryRequest request, @MappingTarget Story story);
}

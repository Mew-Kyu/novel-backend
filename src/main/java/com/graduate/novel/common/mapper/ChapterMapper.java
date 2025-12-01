package com.graduate.novel.common.mapper;

import com.graduate.novel.domain.chapter.Chapter;
import com.graduate.novel.domain.chapter.ChapterDto;
import com.graduate.novel.domain.chapter.CreateChapterRequest;
import com.graduate.novel.domain.chapter.UpdateChapterRequest;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ChapterMapper {

    @Mapping(target = "storyId", source = "story.id")
    ChapterDto toDto(Chapter chapter);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "story", ignore = true)
    @Mapping(target = "crawlStatus", constant = "PENDING")
    @Mapping(target = "translateStatus", constant = "NONE")
    @Mapping(target = "crawlTime", ignore = true)
    @Mapping(target = "translateTime", ignore = true)
    @Mapping(target = "translatedContent", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Chapter toEntity(CreateChapterRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "story", ignore = true)
    @Mapping(target = "crawlStatus", ignore = true)
    @Mapping(target = "translateStatus", ignore = true)
    @Mapping(target = "crawlTime", ignore = true)
    @Mapping(target = "translateTime", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(UpdateChapterRequest request, @MappingTarget Chapter chapter);
}

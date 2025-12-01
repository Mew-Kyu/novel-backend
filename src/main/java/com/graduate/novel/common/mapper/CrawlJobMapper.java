package com.graduate.novel.common.mapper;

import com.graduate.novel.domain.crawljob.CrawlJob;
import com.graduate.novel.domain.crawljob.CrawlJobDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CrawlJobMapper {

    @Mapping(target = "storyId", source = "story.id")
    @Mapping(target = "chapterId", source = "chapter.id")
    CrawlJobDto toDto(CrawlJob crawlJob);
}


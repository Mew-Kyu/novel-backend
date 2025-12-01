package com.graduate.novel.common.mapper;

import com.graduate.novel.domain.history.ReadingHistory;
import com.graduate.novel.domain.history.ReadingHistoryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = StoryMapper.class)
public interface ReadingHistoryMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "chapterId", source = "chapter.id")
    @Mapping(target = "chapterTitle", source = "chapter.title")
    ReadingHistoryDto toDto(ReadingHistory history);
}

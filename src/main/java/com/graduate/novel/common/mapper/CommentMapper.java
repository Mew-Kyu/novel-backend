package com.graduate.novel.common.mapper;

import com.graduate.novel.domain.comment.Comment;
import com.graduate.novel.domain.comment.CommentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommentMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.displayName", target = "userName")
    @Mapping(source = "user.avatarUrl", target = "userAvatarUrl")
    @Mapping(source = "story.id", target = "storyId")
    CommentDto toDto(Comment comment);
}


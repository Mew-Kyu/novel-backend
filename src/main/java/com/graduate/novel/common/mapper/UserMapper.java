package com.graduate.novel.common.mapper;

import com.graduate.novel.domain.user.User;
import com.graduate.novel.domain.user.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    UserDto toDto(User user);
}

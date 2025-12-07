package com.graduate.novel.common.mapper;

import com.graduate.novel.domain.role.Role;
import com.graduate.novel.domain.role.RoleDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RoleMapper {

    RoleDto toDto(Role role);

    List<RoleDto> toDtoList(List<Role> roles);

    Set<RoleDto> toDtoSet(Set<Role> roles);
}


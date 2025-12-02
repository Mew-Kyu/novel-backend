package com.graduate.novel.common.mapper;

import com.graduate.novel.domain.genre.CreateGenreRequest;
import com.graduate.novel.domain.genre.Genre;
import com.graduate.novel.domain.genre.GenreDto;
import com.graduate.novel.domain.genre.UpdateGenreRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GenreMapper {
    GenreDto toDto(Genre genre);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "stories", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Genre toEntity(CreateGenreRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "stories", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(UpdateGenreRequest request, @MappingTarget Genre genre);
}


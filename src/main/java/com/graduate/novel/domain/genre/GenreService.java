package com.graduate.novel.domain.genre;

import com.graduate.novel.common.exception.ResourceNotFoundException;
import com.graduate.novel.common.mapper.GenreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;

    @Transactional(readOnly = true)
    public List<GenreDto> getAllGenres() {
        return genreRepository.findAll().stream()
                .map(genreMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GenreDto getGenreById(Long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found with id: " + id));
        return genreMapper.toDto(genre);
    }

    @Transactional(readOnly = true)
    public GenreDto getGenreByName(String name) {
        Genre genre = genreRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found with name: " + name));
        return genreMapper.toDto(genre);
    }

    @Transactional
    public GenreDto createGenre(CreateGenreRequest request) {
        // Check if genre with same name already exists
        if (genreRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Genre with name '" + request.name() + "' already exists");
        }

        Genre genre = genreMapper.toEntity(request);
        genre = genreRepository.save(genre);
        return genreMapper.toDto(genre);
    }

    @Transactional
    public GenreDto updateGenre(Long id, UpdateGenreRequest request) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found with id: " + id));

        // Check if new name conflicts with existing genre
        if (request.name() != null && !request.name().equals(genre.getName())) {
            if (genreRepository.existsByName(request.name())) {
                throw new IllegalArgumentException("Genre with name '" + request.name() + "' already exists");
            }
        }

        // Update only non-null fields
        if (request.name() != null) {
            genre.setName(request.name());
        }
        if (request.description() != null) {
            genre.setDescription(request.description());
        }

        genre = genreRepository.save(genre);
        return genreMapper.toDto(genre);
    }

    @Transactional
    public void deleteGenre(Long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found with id: " + id));

        // Note: Stories associated with this genre will remain, but the association will be removed
        // due to the cascade settings in the many-to-many relationship
        genreRepository.delete(genre);
    }
}


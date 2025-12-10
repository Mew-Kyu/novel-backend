package com.graduate.novel.controller;

import com.graduate.novel.domain.genre.CreateGenreRequest;
import com.graduate.novel.domain.genre.GenreDetailDto;
import com.graduate.novel.domain.genre.GenreDto;
import com.graduate.novel.domain.genre.GenreService;
import com.graduate.novel.domain.genre.UpdateGenreRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;

    @GetMapping
    public ResponseEntity<List<GenreDto>> getAllGenres() {
        List<GenreDto> genres = genreService.getAllGenres();
        return ResponseEntity.ok(genres);
    }

    /**
     * Get all genres with story counts
     */
    @GetMapping("/with-counts")
    public ResponseEntity<List<GenreDetailDto>> getAllGenresWithCounts() {
        List<GenreDetailDto> genres = genreService.getAllGenresWithCounts();
        return ResponseEntity.ok(genres);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GenreDto> getGenreById(@PathVariable Long id) {
        GenreDto genre = genreService.getGenreById(id);
        return ResponseEntity.ok(genre);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<GenreDto> getGenreByName(@PathVariable String name) {
        GenreDto genre = genreService.getGenreByName(name);
        return ResponseEntity.ok(genre);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<GenreDto> createGenre(@Valid @RequestBody CreateGenreRequest request) {
        GenreDto genre = genreService.createGenre(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(genre);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<GenreDto> updateGenre(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGenreRequest request) {
        GenreDto genre = genreService.updateGenre(id, request);
        return ResponseEntity.ok(genre);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<Void> deleteGenre(@PathVariable Long id) {
        genreService.deleteGenre(id);
        return ResponseEntity.noContent().build();
    }
}


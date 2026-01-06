package com.graduate.novel.domain.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenrePreference {
    private Long genreId;
    private String genreName;
    private Double score;
    private Integer interactionCount;
}


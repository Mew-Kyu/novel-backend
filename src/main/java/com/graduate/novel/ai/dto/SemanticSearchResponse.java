package com.graduate.novel.ai.dto;

import com.graduate.novel.domain.story.StoryDto;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSearchResponse {
    private String query;
    private List<StoryDto> results;
    private Integer totalResults;
}


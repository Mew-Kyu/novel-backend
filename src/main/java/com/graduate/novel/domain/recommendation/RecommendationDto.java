package com.graduate.novel.domain.recommendation;

import com.graduate.novel.domain.story.StoryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Personalized story recommendations for user")
public class RecommendationDto {

    @Schema(description = "List of recommended stories")
    private List<StoryDto> stories;

    @Schema(description = "Recommendation algorithm used")
    private RecommendationType type;

    @Schema(description = "Total number of recommendations")
    private Integer totalCount;

    @Schema(description = "Explanation of why these stories were recommended")
    private String explanation;
}


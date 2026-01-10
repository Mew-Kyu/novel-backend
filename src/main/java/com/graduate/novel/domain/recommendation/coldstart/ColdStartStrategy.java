package com.graduate.novel.domain.recommendation.coldstart;

import com.graduate.novel.domain.story.StoryDto;
import java.util.List;

public interface ColdStartStrategy {

    List<StoryDto> getRecommendations(Long userId, int limit);

    boolean isApplicable(Long userId);

    String getName();

    int getPriority();
}


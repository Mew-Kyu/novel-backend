package com.graduate.novel.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslateStoryRequest {
    private Long storyId;
    private Boolean translateTitle;
    private Boolean translateDescription;

    // If not provided, defaults to true for both
    public Boolean getTranslateTitle() {
        return translateTitle != null ? translateTitle : true;
    }

    public Boolean getTranslateDescription() {
        return translateDescription != null ? translateDescription : true;
    }
}


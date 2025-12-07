package com.graduate.novel.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslateStoryResponse {
    private Long storyId;
    private String originalTitle;
    private String translatedTitle;
    private String originalAuthorName;
    private String translatedAuthorName;
    private String originalDescription;
    private String translatedDescription;
    private String message;
}


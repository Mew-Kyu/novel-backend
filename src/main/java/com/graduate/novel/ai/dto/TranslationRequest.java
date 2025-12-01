package com.graduate.novel.ai.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationRequest {
    private String text;
    private String sourceLanguage;  // Optional: "ja", "en", etc.
    private String targetLanguage;  // Optional: defaults to "vi"
}

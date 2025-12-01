package com.graduate.novel.ai.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSearchRequest {
    private String query;
    private Integer limit;  // Optional: defaults to 10
}


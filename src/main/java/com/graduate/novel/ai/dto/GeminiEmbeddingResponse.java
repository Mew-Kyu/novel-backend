package com.graduate.novel.ai.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiEmbeddingResponse {
    private Embedding embedding;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Embedding {
        private float[] values;
    }
}


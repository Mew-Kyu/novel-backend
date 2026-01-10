package com.graduate.novel.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Facade service to switch between different embedding strategies
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultiEmbeddingService {

    private final GeminiService geminiService;
    private final TfidfEmbeddingService tfidfService;
    private final Word2VecEmbeddingService word2VecService;
    // Note: SbertEmbeddingService removed due to compilation issues - will be added when ONNX/model integration is ready

    /**
     * Generate embedding using specified strategy
     */
    public float[] generateEmbedding(String text, EmbeddingStrategyType strategy) {
        log.debug("Generating embedding with strategy: {}", strategy);

        return switch (strategy) {
            case GEMINI -> {
                // Gemini returns 768-dim embedding
                float[] embedding = geminiService.generateEmbedding(text);
                yield embedding != null ? embedding : new float[768];
            }
            case TFIDF -> tfidfService.generateEmbedding(text);
            case WORD2VEC -> word2VecService.generateEmbedding(text);
            case SBERT -> {
                log.warn("SBERT not yet implemented, falling back to Word2Vec");
                yield word2VecService.generateEmbedding(text);
            }
            case AUTO -> generateWithFallback(text);
        };
    }

    /**
     * Auto strategy with fallback chain
     * Tries Gemini -> Word2Vec -> TF-IDF
     */
    private float[] generateWithFallback(String text) {
        // Try Gemini first (best quality)
        try {
            float[] embedding = geminiService.generateEmbedding(text);
            if (embedding != null && embedding.length > 0) {
                log.debug("Using Gemini embedding");
                return embedding;
            }
        } catch (Exception e) {
            log.warn("Gemini embedding failed: {}", e.getMessage());
        }

        // Fallback to Word2Vec
        try {
            log.debug("Falling back to Word2Vec");
            return word2VecService.generateEmbedding(text);
        } catch (Exception e) {
            log.warn("Word2Vec embedding failed: {}", e.getMessage());
        }

        // Final fallback to TF-IDF (always works)
        log.debug("Using TF-IDF as final fallback");
        return tfidfService.generateEmbedding(text);
    }


    /**
     * Embedding strategy types
     */
    public enum EmbeddingStrategyType {
        GEMINI,      // Cloud-based, high quality, requires API key
        SBERT,       // Local/API, very good quality, sentence-level
        WORD2VEC,    // Local, good quality, word-level
        TFIDF,       // Local, fast, traditional
        AUTO         // Automatic fallback chain
    }

    /**
     * Compare embeddings from different strategies
     */
    public ComparisonResult compareStrategies(String text) {
        log.info("Comparing embedding strategies for text: {}",
            text.length() > 50 ? text.substring(0, 50) + "..." : text);

        ComparisonResult result = new ComparisonResult();

        for (EmbeddingStrategyType strategy : EmbeddingStrategyType.values()) {
            if (strategy == EmbeddingStrategyType.AUTO) continue;

            try {
                long startTime = System.currentTimeMillis();
                float[] embedding = generateEmbedding(text, strategy);
                long duration = System.currentTimeMillis() - startTime;

                result.addResult(strategy, embedding.length, duration);
            } catch (Exception e) {
                log.error("Failed to generate embedding with {}: {}", strategy, e.getMessage());
                result.addError(strategy, e.getMessage());
            }
        }

        return result;
    }

    /**
     * Comparison result DTO
     */
    @lombok.Data
    public static class ComparisonResult {
        private final java.util.Map<EmbeddingStrategyType, StrategyResult> results =
            new java.util.HashMap<>();

        void addResult(EmbeddingStrategyType strategy, int dimension, long durationMs) {
            results.put(strategy, new StrategyResult(dimension, durationMs, null));
        }

        void addError(EmbeddingStrategyType strategy, String error) {
            results.put(strategy, new StrategyResult(0, 0, error));
        }

        @lombok.Data
        @lombok.AllArgsConstructor
        public static class StrategyResult {
            private int dimension;
            private long durationMs;
            private String error;

            public boolean isSuccess() {
                return error == null;
            }
        }
    }
}


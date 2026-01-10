package com.graduate.novel.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Word2Vec-inspired embedding service
 * Simplified implementation using Skip-gram like approach
 *
 * Note: For production, consider using pre-trained models like:
 * - Google's Word2Vec
 * - FastText (better for Vietnamese)
 * - Or train custom model with DL4J/Tensorflow
 */
@Service
@Slf4j
public class Word2VecEmbeddingService implements EmbeddingStrategy {

    private static final int EMBEDDING_DIM = 300;
    private static final int WINDOW_SIZE = 5;

    // In-memory word embeddings (in production, load from pre-trained model)
    private final Map<String, float[]> wordVectors = new HashMap<>();
    private final Random random = new Random(42);

    @Override
    public float[] generateEmbedding(String text) {
        if (text == null || text.isEmpty()) {
            return new float[EMBEDDING_DIM];
        }

        // Tokenize
        List<String> tokens = tokenize(text);

        if (tokens.isEmpty()) {
            return new float[EMBEDDING_DIM];
        }

        // Average word vectors
        float[] documentVector = new float[EMBEDDING_DIM];
        int validWords = 0;

        for (String token : tokens) {
            float[] wordVector = getOrCreateWordVector(token);

            for (int i = 0; i < EMBEDDING_DIM; i++) {
                documentVector[i] += wordVector[i];
            }
            validWords++;
        }

        // Average
        if (validWords > 0) {
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                documentVector[i] /= validWords;
            }
        }

        // L2 normalize
        return normalize(documentVector);
    }

    @Override
    public int getDimension() {
        return EMBEDDING_DIM;
    }

    @Override
    public String getName() {
        return "Word2Vec";
    }

    @Override
    public boolean requiresApi() {
        return false;
    }

    /**
     * Tokenize text
     */
    private List<String> tokenize(String text) {
        String[] tokens = text.toLowerCase()
            .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
            .split("\\s+");

        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            if (token.length() > 2) {
                result.add(token);
            }
        }

        return result;
    }

    /**
     * Get word vector or create random vector for unknown words
     */
    private synchronized float[] getOrCreateWordVector(String word) {
        return wordVectors.computeIfAbsent(word, w -> {
            // Initialize with small random values (Xavier initialization)
            float[] vector = new float[EMBEDDING_DIM];
            float scale = (float) Math.sqrt(2.0 / EMBEDDING_DIM);

            for (int i = 0; i < EMBEDDING_DIM; i++) {
                vector[i] = (random.nextFloat() - 0.5f) * 2 * scale;
            }

            return normalize(vector);
        });
    }

    /**
     * L2 normalize vector
     */
    private float[] normalize(float[] vector) {
        float norm = 0.0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        if (norm > 0) {
            float[] normalized = new float[vector.length];
            for (int i = 0; i < vector.length; i++) {
                normalized[i] = vector[i] / norm;
            }
            return normalized;
        }

        return vector;
    }

    /**
     * Load pre-trained word vectors (for production use)
     * Format: word float1 float2 ... floatN
     */
    public void loadPreTrainedVectors(Map<String, float[]> preTrainedVectors) {
        log.info("Loading {} pre-trained word vectors", preTrainedVectors.size());
        wordVectors.clear();
        wordVectors.putAll(preTrainedVectors);
        log.info("Loaded {} word vectors", wordVectors.size());
    }

    /**
     * Train word vectors using Skip-gram (simplified version)
     * In production, use proper library like DL4J or Gensim
     */
    public void train(List<String> corpus, int epochs) {
        log.info("Training Word2Vec on {} documents for {} epochs", corpus.size(), epochs);

        // This is a placeholder - real Word2Vec training is complex
        // For production, use:
        // - DeepLearning4J's Word2Vec
        // - Pre-trained FastText models
        // - Or call Python Gensim via REST API

        log.warn("Word2Vec training not fully implemented. Using random initialization.");
        log.info("For production, use pre-trained models or dedicated ML libraries.");
    }

    /**
     * Calculate similarity between two words
     */
    public double similarity(String word1, String word2) {
        float[] v1 = getOrCreateWordVector(word1);
        float[] v2 = getOrCreateWordVector(word2);

        return cosineSimilarity(v1, v2);
    }

    /**
     * Cosine similarity
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot; // Already normalized
    }

    /**
     * Get vocabulary size
     */
    public int getVocabularySize() {
        return wordVectors.size();
    }

    /**
     * Reset model
     */
    public void reset() {
        wordVectors.clear();
    }
}


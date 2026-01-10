package com.graduate.novel.ai.service;

public interface EmbeddingStrategy {

    float[] generateEmbedding(String text);

    int getDimension();

    String getName();

    boolean requiresApi();
}


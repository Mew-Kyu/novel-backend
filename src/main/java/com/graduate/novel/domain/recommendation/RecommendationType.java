package com.graduate.novel.domain.recommendation;

public enum RecommendationType {
    COLLABORATIVE,      // Based on similar users
    CONTENT_BASED,      // Based on story content/genres
    SEMANTIC,           // Based on embeddings similarity
    TRENDING,           // Popular stories
    HYBRID              // Combined approach
}


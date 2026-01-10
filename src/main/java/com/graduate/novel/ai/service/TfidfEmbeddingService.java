package com.graduate.novel.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TF-IDF (Term Frequency-Inverse Document Frequency) embedding strategy
 * Lightweight, fast, no API required
 * Good for cold-start scenarios
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TfidfEmbeddingService implements EmbeddingStrategy {

    // In-memory document frequency storage (in production, use Redis or database)
    private final Map<String, Integer> documentFrequency = new HashMap<>();
    private int totalDocuments = 0;

    // Fixed vocabulary size for consistent embedding dimension
    private static final int VOCAB_SIZE = 500;
    private final List<String> vocabulary = new ArrayList<>();

    @Override
    public float[] generateEmbedding(String text) {
        if (text == null || text.isEmpty()) {
            return new float[VOCAB_SIZE];
        }

        // Tokenize and calculate term frequency
        Map<String, Integer> termFrequency = calculateTermFrequency(text);

        // Build vocabulary if needed
        if (vocabulary.isEmpty()) {
            buildVocabulary(termFrequency.keySet());
        }

        // Update document frequencies
        updateDocumentFrequencies(termFrequency.keySet());

        // Calculate TF-IDF vector
        return calculateTfIdfVector(termFrequency);
    }

    @Override
    public int getDimension() {
        return VOCAB_SIZE;
    }

    @Override
    public String getName() {
        return "TF-IDF";
    }

    @Override
    public boolean requiresApi() {
        return false;
    }

    /**
     * Calculate term frequency for a document
     */
    private Map<String, Integer> calculateTermFrequency(String text) {
        Map<String, Integer> tf = new HashMap<>();

        // Simple tokenization (lowercase, split by non-word chars)
        String[] tokens = text.toLowerCase()
            .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
            .split("\\s+");

        for (String token : tokens) {
            if (token.length() > 2) { // Filter short words
                tf.merge(token, 1, Integer::sum);
            }
        }

        return tf;
    }

    /**
     * Build fixed vocabulary from most common terms
     */
    private synchronized void buildVocabulary(Set<String> terms) {
        if (!vocabulary.isEmpty()) return;

        // In production, this should be pre-computed from corpus
        // For now, just add terms as they appear
        vocabulary.addAll(terms.stream()
            .limit(VOCAB_SIZE)
            .collect(Collectors.toList()));

        // Pad with empty slots if needed
        while (vocabulary.size() < VOCAB_SIZE) {
            vocabulary.add("_EMPTY_" + vocabulary.size());
        }

        log.info("Built TF-IDF vocabulary with {} terms", vocabulary.size());
    }

    /**
     * Update document frequencies
     */
    private synchronized void updateDocumentFrequencies(Set<String> terms) {
        totalDocuments++;

        for (String term : terms) {
            documentFrequency.merge(term, 1, Integer::sum);
        }
    }

    /**
     * Calculate TF-IDF vector for a document
     */
    private float[] calculateTfIdfVector(Map<String, Integer> termFrequency) {
        float[] vector = new float[VOCAB_SIZE];

        // Max term frequency for normalization
        int maxTf = termFrequency.values().stream()
            .max(Integer::compareTo)
            .orElse(1);

        for (int i = 0; i < vocabulary.size(); i++) {
            String term = vocabulary.get(i);

            if (termFrequency.containsKey(term)) {
                // TF: normalized term frequency
                double tf = 0.5 + 0.5 * ((double) termFrequency.get(term) / maxTf);

                // IDF: log(total docs / docs containing term)
                int df = documentFrequency.getOrDefault(term, 1);
                double idf = Math.log((double) totalDocuments / df);

                // TF-IDF
                vector[i] = (float) (tf * idf);
            } else {
                vector[i] = 0.0f;
            }
        }

        // L2 normalization
        float norm = 0.0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }

    /**
     * Pre-train vocabulary from a corpus (for production use)
     */
    public void trainVocabulary(List<String> documents) {
        log.info("Training TF-IDF vocabulary from {} documents", documents.size());

        // Count term frequencies across all documents
        Map<String, Integer> globalTermFreq = new HashMap<>();

        for (String doc : documents) {
            Map<String, Integer> docTf = calculateTermFrequency(doc);
            docTf.keySet().forEach(term ->
                globalTermFreq.merge(term, 1, Integer::sum)
            );
        }

        // Select top VOCAB_SIZE terms by document frequency
        vocabulary.clear();
        vocabulary.addAll(globalTermFreq.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(VOCAB_SIZE)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList()));

        // Update document frequencies
        totalDocuments = documents.size();
        documentFrequency.clear();
        documentFrequency.putAll(globalTermFreq);

        log.info("Trained vocabulary with {} terms from {} documents",
            vocabulary.size(), totalDocuments);
    }

    /**
     * Reset the model (for testing)
     */
    public void reset() {
        vocabulary.clear();
        documentFrequency.clear();
        totalDocuments = 0;
    }
}


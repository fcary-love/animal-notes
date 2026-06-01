package com.pettimeline.ai.rag;

import com.pettimeline.model.entity.KnowledgeChunk;

import java.util.*;

/**
 * Simple BM25 implementation for Chinese text retrieval.
 * Uses character bigram tokenization for Chinese, space-split for English.
 */
public class Bm25Service {

    private static final double K1 = 1.5;
    private static final double B = 0.75;

    public static List<Scored> score(List<KnowledgeChunk> chunks, String query) {
        List<String> queryTerms = tokenize(query);
        if (queryTerms.isEmpty()) return List.of();

        // Build inverted index
        Map<String, Map<Long, Integer>> termFreqs = new HashMap<>();
        Map<Long, Integer> docLengths = new HashMap<>();
        double totalLen = 0;

        for (KnowledgeChunk c : chunks) {
            List<String> terms = tokenize(c.getContent());
            docLengths.put(c.getId(), terms.size());
            totalLen += terms.size();
            for (String t : terms) {
                termFreqs.computeIfAbsent(t, k -> new HashMap<>())
                        .merge(c.getId(), 1, Integer::sum);
            }
        }

        double avgdl = totalLen / Math.max(1, chunks.size());
        int N = chunks.size();

        // Score each document
        List<Scored> results = new ArrayList<>();
        for (KnowledgeChunk c : chunks) {
            double score = 0;
            int dl = docLengths.getOrDefault(c.getId(), 1);
            for (String qt : queryTerms) {
                Map<Long, Integer> posting = termFreqs.get(qt);
                if (posting == null) continue;
                int df = posting.size();
                int tf = posting.getOrDefault(c.getId(), 0);
                if (tf == 0) continue;
                double idf = Math.log((N - df + 0.5) / (df + 0.5) + 1);
                double numerator = tf * (K1 + 1);
                double denominator = tf + K1 * (1 - B + B * dl / avgdl);
                score += idf * numerator / denominator;
            }
            results.add(new Scored(c, score));
        }

        results.sort((a, b) -> Double.compare(b.score, a.score));
        return results;
    }

    static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> tokens = new ArrayList<>();
        // Simple bigram tokenization for Chinese
        String cleaned = text.replaceAll("[\\p{P}\\s]+", "");
        for (int i = 0; i < cleaned.length() - 1; i++) {
            tokens.add(cleaned.substring(i, i + 2));
        }
        // Also add unigrams
        for (char c : cleaned.toCharArray()) {
            tokens.add(String.valueOf(c));
        }
        return tokens;
    }

    public record Scored(KnowledgeChunk chunk, double score) {}
}

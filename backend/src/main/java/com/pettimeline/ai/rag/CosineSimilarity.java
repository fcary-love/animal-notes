package com.pettimeline.ai.rag;

import java.util.List;

public class CosineSimilarity {

    public static double compute(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) {
            throw new IllegalArgumentException("向量维度不一致");
        }
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}

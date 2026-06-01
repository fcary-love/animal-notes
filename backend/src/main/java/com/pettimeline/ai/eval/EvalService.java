package com.pettimeline.ai.eval;

import com.pettimeline.ai.rag.Bm25Service;
import com.pettimeline.ai.rag.CosineSimilarity;
import com.pettimeline.ai.rag.EmbeddingService;
import com.pettimeline.ai.rag.VectorIndexService;
import com.pettimeline.mapper.KnowledgeChunkMapper;
import com.pettimeline.model.entity.KnowledgeChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class EvalService {

    private final EmbeddingService embeddingService;
    private final VectorIndexService vectorIndexService;
    private final KnowledgeChunkMapper chunkMapper;

    public EvalReport runFullEval() {
        List<KnowledgeChunk> allChunks = chunkMapper.selectList(null);
        EvalReport report = new EvalReport();
        report.total = EvalDataset.DATASET.size();
        report.redisAvailable = vectorIndexService.isAvailable();

        for (var qa : EvalDataset.DATASET) {
            List<Double> qVec = embeddingService.embed(qa.question());

            // Mode 1: Keyword (BM25)
            EvalReport.ModeResult kw = evaluateKeyword(allChunks, qa);
            report.keyword.add(kw);

            // Mode 2: Vector (Redis or brute-force)
            EvalReport.ModeResult vec = evaluateVector(qVec, allChunks, qa);
            report.vector.add(vec);

            // Mode 3: Hybrid
            EmbeddingService.QueryHolder.set(qa.question());
            try {
                List<EmbeddingService.ScoredChunk> hybrid = embeddingService.searchChunks(qVec, 5, EmbeddingService.SearchMode.HYBRID);
                EvalReport.ModeResult hr = computeMetrics(hybrid, qa, "HYBRID");
                report.hybrid.add(hr);
            } finally {
                EmbeddingService.QueryHolder.remove();
            }
        }

        report.computeAverages();
        return report;
    }

    private EvalReport.ModeResult evaluateKeyword(List<KnowledgeChunk> all, EvalDataset.QAPair qa) {
        List<Bm25Service.Scored> scored = Bm25Service.score(all, qa.question());
        List<EmbeddingService.ScoredChunk> wrapped = scored.stream()
                .map(s -> new EmbeddingService.ScoredChunk(s.chunk(), s.score()))
                .limit(5).toList();
        return computeMetrics(wrapped, qa, "KEYWORD");
    }

    private EvalReport.ModeResult evaluateVector(List<Double> qVec, List<KnowledgeChunk> all, EvalDataset.QAPair qa) {
        // Try Redis first, fallback to brute-force
        List<VectorIndexService.VecResult> redisResults = vectorIndexService.searchChunks(qVec, 5);
        List<EmbeddingService.ScoredChunk> wrapped;
        if (!redisResults.isEmpty()) {
            wrapped = redisResults.stream().map(r -> {
                KnowledgeChunk c = findChunk(all, r.content());
                return new EmbeddingService.ScoredChunk(c, r.score());
            }).toList();
        } else {
            // Brute-force cosine
            List<EmbeddingService.ScoredChunk> scored = new ArrayList<>();
            for (KnowledgeChunk c : all) {
                List<Double> cv = parseVector(c.getEmbedding());
                if (cv == null) continue;
                double s = CosineSimilarity.compute(qVec, cv);
                scored.add(new EmbeddingService.ScoredChunk(c, s));
            }
            scored.sort((a, b) -> Double.compare(b.score(), a.score()));
            wrapped = scored.subList(0, Math.min(5, scored.size()));
        }
        return computeMetrics(wrapped, qa, "VECTOR");
    }

    private EvalReport.ModeResult computeMetrics(List<EmbeddingService.ScoredChunk> results, EvalDataset.QAPair qa, String mode) {
        boolean hitAt1 = false, hitAt3 = false, hitAt5 = false;
        double reciprocalRank = 0;
        int rank = 0;

        for (EmbeddingService.ScoredChunk sc : results) {
            rank++;
            if (sc.chunk() == null) continue;
            String content = sc.chunk().getContent();
            boolean relevant = qa.expectedKeywords().stream()
                    .anyMatch(kw -> content.contains(kw));
            if (relevant) {
                if (rank == 1) hitAt1 = true;
                if (rank <= 3) hitAt3 = true;
                if (rank <= 5) hitAt5 = true;
                if (reciprocalRank == 0) reciprocalRank = 1.0 / rank;
            }
        }

        EvalReport.ModeResult r = new EvalReport.ModeResult();
        r.mode = mode;
        r.question = qa.question();
        r.hitAt1 = hitAt1;
        r.hitAt3 = hitAt3;
        r.hitAt5 = hitAt5;
        r.mrr = reciprocalRank;
        r.latencyMs = 0; // populated by caller
        return r;
    }

    private KnowledgeChunk findChunk(List<KnowledgeChunk> all, String content) {
        return all.stream().filter(c -> c.getContent().equals(content)).findFirst().orElse(null);
    }

    private List<Double> parseVector(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return cn.hutool.json.JSONUtil.toList(json, Double.class);
        } catch (Exception e) {
            return null;
        }
    }
}

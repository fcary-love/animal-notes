package com.pettimeline.ai.rag;

import cn.hutool.json.JSONUtil;
import com.pettimeline.mapper.KnowledgeChunkMapper;
import com.pettimeline.mapper.MomentEmbeddingMapper;
import com.pettimeline.model.entity.KnowledgeChunk;
import com.pettimeline.model.entity.MomentEmbedding;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final KnowledgeChunkMapper chunkMapper;
    private final MomentEmbeddingMapper momentEmbeddingMapper;
    private final VectorIndexService vectorIndexService;

    public enum SearchMode { KEYWORD, VECTOR, HYBRID }

    public List<Double> embed(String text) {
        Embedding emb = embeddingModel.embed(text).content();
        return emb.vectorAsList().stream().map(Float::doubleValue).toList();
    }

    public void embedAndSaveChunk(KnowledgeChunk chunk) {
        List<Double> vec = embed(chunk.getContent());
        chunk.setEmbedding(JSONUtil.toJsonStr(vec));
        chunkMapper.updateById(chunk);
        vectorIndexService.indexChunk(chunk.getId(), chunk.getContent(), vec);
    }

    public void embedAndSaveMoment(Long momentId, String content) {
        List<Double> vec = embed(content != null && !content.isEmpty() ? content : " ");
        MomentEmbedding me = momentEmbeddingMapper.findByMomentId(momentId);
        if (me == null) {
            me = new MomentEmbedding();
            me.setMomentId(momentId);
            me.setEmbedding(JSONUtil.toJsonStr(vec));
            momentEmbeddingMapper.insert(me);
        } else {
            me.setEmbedding(JSONUtil.toJsonStr(vec));
            momentEmbeddingMapper.updateById(me);
        }
        vectorIndexService.indexMoment(momentId, content, vec);
    }

    public void deleteMomentEmbedding(Long momentId) {
        momentEmbeddingMapper.deleteByMomentId(momentId);
        vectorIndexService.deleteMoment(momentId);
    }

    // ===== Search (tri-mode) =====

    public List<ScoredChunk> searchChunks(List<Double> queryVec, int topK, SearchMode mode) {
        return switch (mode) {
            case VECTOR -> vectorSearch(queryVec, topK);
            case KEYWORD -> keywordSearch(queryVec, topK);
            case HYBRID -> hybridSearch(queryVec, topK);
        };
    }

    public List<ScoredChunk> searchChunks(List<Double> queryVec, int topK) {
        return searchChunks(queryVec, topK, SearchMode.VECTOR);
    }

    // ===== Vector (Redis Stack with brute-force fallback) =====

    private List<ScoredChunk> vectorSearch(List<Double> queryVec, int topK) {
        List<VectorIndexService.VecResult> results = vectorIndexService.searchChunks(queryVec, topK);
        if (!results.isEmpty()) {
            List<KnowledgeChunk> all = chunkMapper.selectList(null);
            return results.stream().map(r -> {
                KnowledgeChunk c = findChunk(all, r.content());
                return new ScoredChunk(c, r.score());
            }).toList();
        }
        // Fallback: brute-force cosine similarity
        return bruteForceSearch(queryVec, topK);
    }

    private List<ScoredChunk> bruteForceSearch(List<Double> queryVec, int topK) {
        List<KnowledgeChunk> all = chunkMapper.selectList(null);
        List<ScoredChunk> scored = new ArrayList<>();
        for (KnowledgeChunk chunk : all) {
            List<Double> chunkVec = getChunkVector(chunk);
            if (chunkVec == null || chunkVec.isEmpty()) continue;
            double score = CosineSimilarity.compute(queryVec, chunkVec);
            scored.add(new ScoredChunk(chunk, score));
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        return scored.subList(0, Math.min(topK, scored.size()));
    }

    private List<Double> getChunkVector(KnowledgeChunk chunk) {
        if (chunk.getEmbedding() == null || chunk.getEmbedding().isEmpty()) return null;
        try {
            return cn.hutool.json.JSONUtil.toList(chunk.getEmbedding(), Double.class);
        } catch (Exception e) {
            return null;
        }
    }

    // ===== Keyword (BM25) =====

    private List<ScoredChunk> keywordSearch(List<Double> queryVec, int topK) {
        List<KnowledgeChunk> all = chunkMapper.selectList(null);
        // We need the query text; pass via a thread-local or method param
        List<Bm25Service.Scored> bm25 = Bm25Service.score(all, QueryHolder.get());
        return bm25.stream().limit(topK)
                .map(s -> new ScoredChunk(s.chunk(), s.score())).toList();
    }

    // ===== Hybrid (Reciprocal Rank Fusion) =====

    private List<ScoredChunk> hybridSearch(List<Double> queryVec, int topK) {
        List<KnowledgeChunk> all = chunkMapper.selectList(null);
        String query = QueryHolder.get();

        List<VectorIndexService.VecResult> vecResults = vectorIndexService.searchChunks(queryVec, Math.min(30, all.size()));
        List<Bm25Service.Scored> bm25Results = Bm25Service.score(all, query);

        Map<Long, Double> rrf = new LinkedHashMap<>();
        int rank = 0;
        for (var r : vecResults) {
            KnowledgeChunk c = findChunk(all, r.content());
            if (c != null) rrf.merge(c.getId(), 1.0 / (60 + rank), Double::sum);
            rank++;
        }
        rank = 0;
        for (var s : bm25Results) {
            rrf.merge(s.chunk().getId(), 1.0 / (60 + rank), Double::sum);
            rank++;
        }

        return rrf.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(e -> new ScoredChunk(chunkMapper.selectById(e.getKey()), e.getValue()))
                .toList();
    }

    private KnowledgeChunk findChunk(List<KnowledgeChunk> all, String content) {
        return all.stream().filter(c -> c.getContent().equals(content)).findFirst().orElse(null);
    }

    public record ScoredChunk(KnowledgeChunk chunk, double score) {}

    // Thread-local to pass query text to BM25/hybrid search
    public static final ThreadLocal<String> QueryHolder = new ThreadLocal<>();
}

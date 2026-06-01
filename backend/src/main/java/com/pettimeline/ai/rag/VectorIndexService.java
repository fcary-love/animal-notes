package com.pettimeline.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.args.SortingOrder;
import redis.clients.jedis.search.FTSearchParams;
import redis.clients.jedis.search.SearchResult;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorIndexService {

    private final JedisPooled jedis;

    static final String IDX_CHUNKS = "idx:knowledge";
    static final String IDX_MOMENTS = "idx:moments";
    private static final String PREFIX_CHUNK = "kn:chunk:";
    private static final String PREFIX_MOMENT = "emb:moment:";

    /**
     * Run this once before first use:
     * redis-cli FT.CREATE idx:knowledge ON HASH PREFIX 1 kn:chunk: SCHEMA content TEXT embedding VECTOR HNSW 6 TYPE FLOAT32 DIM 1536 DISTANCE_METRIC COSINE
     * redis-cli FT.CREATE idx:moments ON HASH PREFIX 1 emb:moment: SCHEMA content TEXT embedding VECTOR HNSW 6 TYPE FLOAT32 DIM 1536 DISTANCE_METRIC COSINE
     */

    // ===== Index Operations =====

    public void indexChunk(Long chunkId, String content, List<Double> embedding) {
        if (!isAvailable()) return;
        try {
            String key = PREFIX_CHUNK + chunkId;
            Map<byte[], byte[]> map = new HashMap<>();
            map.put("content".getBytes(), (content != null ? content : "").getBytes());
            map.put("embedding".getBytes(), floatsToBytes(embedding));
            jedis.hset(key.getBytes(), map);
        } catch (Exception e) {
            log.debug("Redis index skip: {}", e.getMessage());
        }
    }

    public void indexMoment(Long momentId, String content, List<Double> embedding) {
        if (!isAvailable()) return;
        try {
            String key = PREFIX_MOMENT + momentId;
            Map<byte[], byte[]> map = new HashMap<>();
            map.put("content".getBytes(), (content != null ? content : "").getBytes());
            map.put("embedding".getBytes(), floatsToBytes(embedding));
            jedis.hset(key.getBytes(), map);
        } catch (Exception e) {
            log.debug("Redis index skip: {}", e.getMessage());
        }
    }

    public void deleteChunk(Long chunkId) {
        jedis.del(PREFIX_CHUNK + chunkId);
    }

    public void deleteMoment(Long momentId) {
        jedis.del(PREFIX_MOMENT + momentId);
    }

    // ===== Vector Search =====

    public List<VecResult> searchChunks(List<Double> queryVec, int topK) {
        return search(IDX_CHUNKS, queryVec, topK);
    }

    public List<VecResult> searchMoments(List<Double> queryVec, int topK) {
        return search(IDX_MOMENTS, queryVec, topK);
    }

    public boolean isAvailable() {
        try {
            jedis.ftInfo(IDX_CHUNKS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private List<VecResult> search(String index, List<Double> queryVec, int topK) {
        if (!isAvailable()) return List.of();

        byte[] blob = floatsToBytes(queryVec);
        String q = "*=>[KNN " + topK + " @embedding $vec AS score]";

        try {
            SearchResult result = jedis.ftSearch(index, q,
                    FTSearchParams.searchParams()
                            .addParam("vec", blob)
                            .returnFields("score", "content")
                            .sortBy("score", SortingOrder.ASC)
                            .limit(0, topK)
                            .dialect(2));

            List<VecResult> list = new ArrayList<>();
            for (var doc : result.getDocuments()) {
                double raw = Double.parseDouble(doc.getString("score"));
                double score = 1.0 - (raw / 2.0);
                list.add(new VecResult(doc.getString("content"), Math.round(score * 10000.0) / 10000.0));
            }
            return list;
        } catch (Exception e) {
            log.warn("Redis search error: {}", e.getMessage());
            return List.of();
        }
    }

    // ===== Utility =====

    static byte[] floatsToBytes(List<Double> values) {
        ByteBuffer buf = ByteBuffer.allocate(values.size() * Float.BYTES);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        for (double v : values) buf.putFloat((float) v);
        return buf.array();
    }

    public record VecResult(String content, double score) {}
}

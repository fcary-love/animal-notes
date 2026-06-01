package com.pettimeline.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pettimeline.ai.rag.DocumentChunker;
import com.pettimeline.ai.rag.EmbeddingService;
import com.pettimeline.exception.BusinessException;
import com.pettimeline.mapper.KnowledgeChunkMapper;
import com.pettimeline.mapper.KnowledgeDocumentMapper;
import com.pettimeline.model.entity.KnowledgeChunk;
import com.pettimeline.model.entity.KnowledgeDocument;
import com.pettimeline.model.vo.KnowledgeDocumentVO;
import com.pettimeline.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeDocumentMapper docMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;

    @Override
    public KnowledgeDocumentVO addDocument(String title, String content, String category, String source) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(title);
        doc.setContent(content);
        doc.setCategory(category);
        doc.setSource(source);
        docMapper.insert(doc);

        List<String> chunks = DocumentChunker.chunk(content);
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocumentId(doc.getId());
            chunk.setChunkIndex(i);
            chunk.setContent(chunks.get(i));
            chunkMapper.insert(chunk);
            try {
                embeddingService.embedAndSaveChunk(chunk);
            } catch (Exception e) {
                // Embedding may fail if AI key is not configured; document is still saved
                log.warn("Chunk embedding failed for doc {}: {}", doc.getId(), e.getMessage());
            }
        }

        return toVO(doc);
    }

    @Override
    public List<KnowledgeDocumentVO> listDocuments() {
        return docMapper.selectList(new LambdaQueryWrapper<KnowledgeDocument>()
                        .orderByDesc(KnowledgeDocument::getCreatedAt))
                .stream().map(this::toVO).toList();
    }

    @Override
    public void deleteDocument(Long id) {
        KnowledgeDocument doc = docMapper.selectById(id);
        if (doc == null) throw new BusinessException(404, "文档不存在");
        chunkMapper.deleteByDocumentId(id);
        docMapper.deleteById(id);
    }

    @Override
    public void reindexAll() {
        chunkMapper.delete(new LambdaQueryWrapper<>() {});
        List<KnowledgeDocument> docs = docMapper.selectList(null);
        for (KnowledgeDocument doc : docs) {
            List<String> chunks = DocumentChunker.chunk(doc.getContent());
            for (int i = 0; i < chunks.size(); i++) {
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setDocumentId(doc.getId());
                chunk.setChunkIndex(i);
                chunk.setContent(chunks.get(i));
                chunkMapper.insert(chunk);
                try {
                    embeddingService.embedAndSaveChunk(chunk);
                } catch (Exception e) {
                    log.warn("Chunk embedding failed for doc {}: {}", doc.getId(), e.getMessage());
                }
            }
        }
    }

    private KnowledgeDocumentVO toVO(KnowledgeDocument doc) {
        KnowledgeDocumentVO vo = new KnowledgeDocumentVO();
        vo.setId(doc.getId());
        vo.setTitle(doc.getTitle());
        vo.setContent(doc.getContent());
        vo.setCategory(doc.getCategory());
        vo.setSource(doc.getSource());
        vo.setChunkCount(chunkMapper.findByDocumentId(doc.getId()).size());
        vo.setCreatedAt(doc.getCreatedAt());
        return vo;
    }
}

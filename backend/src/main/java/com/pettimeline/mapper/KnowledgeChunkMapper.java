package com.pettimeline.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pettimeline.model.entity.KnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunk> {

    default List<KnowledgeChunk> findByDocumentId(Long documentId) {
        return selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getDocumentId, documentId)
                .orderByAsc(KnowledgeChunk::getChunkIndex));
    }

    default void deleteByDocumentId(Long documentId) {
        delete(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getDocumentId, documentId));
    }
}

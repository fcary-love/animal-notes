package com.pettimeline.service;

import com.pettimeline.model.vo.KnowledgeDocumentVO;

import java.util.List;

public interface KnowledgeService {
    KnowledgeDocumentVO addDocument(String title, String content, String category, String source);
    List<KnowledgeDocumentVO> listDocuments();
    void deleteDocument(Long id);
    void reindexAll();
}

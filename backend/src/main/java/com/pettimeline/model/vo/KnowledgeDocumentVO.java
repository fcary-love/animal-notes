package com.pettimeline.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeDocumentVO {
    private Long id;
    private String title;
    private String content;
    private String category;
    private String source;
    private int chunkCount;
    private LocalDateTime createdAt;
}

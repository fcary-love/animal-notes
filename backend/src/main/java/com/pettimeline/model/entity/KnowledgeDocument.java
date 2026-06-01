package com.pettimeline.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_documents")
public class KnowledgeDocument {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String content;
    private String category;
    private String source;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}

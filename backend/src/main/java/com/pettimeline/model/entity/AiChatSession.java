package com.pettimeline.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_chat_sessions")
public class AiChatSession {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long petId;
    private String title;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}

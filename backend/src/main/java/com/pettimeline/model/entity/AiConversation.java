package com.pettimeline.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_conversations")
public class AiConversation {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long petId;
    private Long sessionId;
    private String role;
    private String content;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;
}

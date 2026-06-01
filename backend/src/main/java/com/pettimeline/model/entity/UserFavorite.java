package com.pettimeline.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_favorites")
public class UserFavorite {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long documentId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

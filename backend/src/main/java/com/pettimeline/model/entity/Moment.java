package com.pettimeline.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("moments")
public class Moment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long petId;
    private String content;
    private String photos;
    private LocalDateTime occurredAt;
    private Integer isMilestone;
    private String milestoneLabel;
    private String location;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}

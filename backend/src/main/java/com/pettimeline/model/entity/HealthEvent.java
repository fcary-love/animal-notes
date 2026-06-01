package com.pettimeline.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_events")
public class HealthEvent {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long petId;
    private String eventType;
    private String title;
    private String description;
    private LocalDate eventDate;
    private LocalDate nextDate;
    private String veterinarian;
    private BigDecimal cost;
    private String photos;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}

package com.pettimeline.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("diet_records")
public class DietRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long petId;
    private String foodType;
    private String foodName;
    private BigDecimal amount;
    private BigDecimal calories;
    private String mealType;
    private LocalDateTime recordedAt;
    private String note;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

package com.pettimeline.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("weight_records")
public class WeightRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long petId;
    private BigDecimal weight;
    private LocalDate recordedAt;
    private String note;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

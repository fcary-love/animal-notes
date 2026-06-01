package com.pettimeline.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DietRecordVO {
    private Long id;
    private Long petId;
    private String foodType;
    private String foodName;
    private BigDecimal amount;
    private BigDecimal calories;
    private String mealType;
    private LocalDateTime recordedAt;
    private String note;
    private LocalDateTime createdAt;
}

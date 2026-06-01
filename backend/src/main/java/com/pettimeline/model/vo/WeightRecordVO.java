package com.pettimeline.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class WeightRecordVO {
    private Long id;
    private Long petId;
    private BigDecimal weight;
    private LocalDate recordedAt;
    private String note;
    private LocalDateTime createdAt;
}

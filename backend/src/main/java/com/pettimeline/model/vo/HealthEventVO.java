package com.pettimeline.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class HealthEventVO {
    private Long id;
    private Long petId;
    private String eventType;
    private String title;
    private String description;
    private LocalDate eventDate;
    private LocalDate nextDate;
    private String veterinarian;
    private BigDecimal cost;
    private List<String> photos;
    private String status;
    private LocalDateTime createdAt;
}

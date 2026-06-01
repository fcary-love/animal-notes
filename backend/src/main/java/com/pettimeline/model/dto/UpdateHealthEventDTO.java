package com.pettimeline.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateHealthEventDTO {
    private String eventType;

    @Size(max = 100)
    private String title;

    @Size(max = 2000)
    private String description;

    private String eventDate;

    private String nextDate;

    @Size(max = 50)
    private String veterinarian;

    private BigDecimal cost;

    private List<String> photos;

    private String status;
}

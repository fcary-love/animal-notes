package com.pettimeline.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateDietDTO {
    @Size(max = 50)
    private String foodType;

    @Size(max = 100)
    private String foodName;

    private BigDecimal amount;
    private BigDecimal calories;

    @Size(max = 20)
    private String mealType;

    private String recordedAt;

    @Size(max = 500)
    private String note;
}

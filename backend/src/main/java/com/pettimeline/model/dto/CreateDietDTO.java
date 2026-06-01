package com.pettimeline.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateDietDTO {
    @NotBlank(message = "食物类型不能为空")
    @Size(max = 50)
    private String foodType;

    @NotBlank(message = "食物名称不能为空")
    @Size(max = 100)
    private String foodName;

    private BigDecimal amount;
    private BigDecimal calories;

    @NotBlank(message = "餐次不能为空")
    @Size(max = 20)
    private String mealType;

    @NotNull(message = "记录时间不能为空")
    private String recordedAt;

    @Size(max = 500)
    private String note;
}

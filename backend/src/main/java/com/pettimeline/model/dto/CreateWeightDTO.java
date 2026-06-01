package com.pettimeline.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateWeightDTO {
    @NotNull(message = "体重不能为空")
    private BigDecimal weight;

    @NotNull(message = "记录日期不能为空")
    private String recordedAt;

    @Size(max = 200)
    private String note;
}

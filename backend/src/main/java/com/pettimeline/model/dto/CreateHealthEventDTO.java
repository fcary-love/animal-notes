package com.pettimeline.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateHealthEventDTO {
    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "事件日期不能为空")
    private String eventDate;

    private String nextDate;

    @Size(max = 50)
    private String veterinarian;

    private BigDecimal cost;

    private List<String> photos;

    private String status;
}

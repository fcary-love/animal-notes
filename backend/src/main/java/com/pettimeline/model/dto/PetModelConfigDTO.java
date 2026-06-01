package com.pettimeline.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PetModelConfigDTO {
    @NotBlank(message = "模型配置不能为空")
    private String modelConfig;
}

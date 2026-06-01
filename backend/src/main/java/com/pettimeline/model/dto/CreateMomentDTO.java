package com.pettimeline.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class CreateMomentDTO {
    @Size(max = 2000, message = "内容最长2000字")
    private String content;

    private List<String> photos;

    @NotNull(message = "事件时间不能为空")
    private String occurredAt;

    private Boolean isMilestone;

    @Size(max = 30)
    private String milestoneLabel;

    @Size(max = 100)
    private String location;
}

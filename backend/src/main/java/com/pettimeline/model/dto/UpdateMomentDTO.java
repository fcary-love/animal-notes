package com.pettimeline.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class UpdateMomentDTO {
    @Size(max = 2000)
    private String content;

    private List<String> photos;

    private String occurredAt;

    @Size(max = 30)
    private String milestoneLabel;

    @Size(max = 100)
    private String location;

    private Boolean isMilestone;
}

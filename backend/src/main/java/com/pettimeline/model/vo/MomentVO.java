package com.pettimeline.model.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MomentVO {
    private Long id;
    private Long petId;
    private String content;
    private List<String> photos;
    private LocalDateTime occurredAt;
    private Boolean isMilestone;
    private String milestoneLabel;
    private String location;
    private LocalDateTime createdAt;
}

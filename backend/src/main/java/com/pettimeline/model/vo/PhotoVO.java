package com.pettimeline.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PhotoVO {
    private Long momentId;
    private String photoUrl;
    private String content;
    private LocalDateTime occurredAt;
    private Boolean isMilestone;
    private String milestoneLabel;
    private String location;
}

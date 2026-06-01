package com.pettimeline.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchResultVO {
    private Long momentId;
    private String content;
    private String occurredAt;
    private String milestoneLabel;
    private double score;
}

package com.pettimeline.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatVO {
    private String answer;
    private List<String> sources;
    private String mode;
    private Long elapsedMs;
}

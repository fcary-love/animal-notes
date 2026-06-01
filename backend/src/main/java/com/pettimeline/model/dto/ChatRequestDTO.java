package com.pettimeline.model.dto;

import lombok.Data;

@Data
public class ChatRequestDTO {
    private String question;
    private Long petId;
    private Long sessionId;
    private String searchMode; // VECTOR / KEYWORD / HYBRID
}

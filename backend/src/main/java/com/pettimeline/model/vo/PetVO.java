package com.pettimeline.model.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PetVO {
    private Long id;
    private Long userId;
    private String name;
    private String species;
    private String breed;
    private LocalDate birthday;
    private String avatarUrl;
    private String bio;
    private String modelConfig;
    private LocalDateTime modelUpdatedAt;
    private LocalDateTime createdAt;
}

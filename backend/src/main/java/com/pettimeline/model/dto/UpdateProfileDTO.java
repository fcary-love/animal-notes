package com.pettimeline.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileDTO {
    @Size(max = 50, message = "昵称最长50位")
    private String nickname;
}

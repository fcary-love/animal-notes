package com.pettimeline.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePetDTO {
    @NotBlank(message = "宠物名不能为空")
    @Size(max = 50)
    private String name;

    @NotBlank(message = "物种不能为空")
    @Size(max = 20)
    private String species;

    @Size(max = 50)
    private String breed;

    private String birthday;

    @Size(max = 200)
    private String bio;
}

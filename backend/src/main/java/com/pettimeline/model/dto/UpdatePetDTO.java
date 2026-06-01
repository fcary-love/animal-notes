package com.pettimeline.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePetDTO {
    @Size(max = 50)
    private String name;

    @Size(max = 20)
    private String species;

    @Size(max = 50)
    private String breed;

    private String birthday;

    @Size(max = 200)
    private String bio;
}

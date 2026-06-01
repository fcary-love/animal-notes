package com.pettimeline.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDTO {
    @NotBlank(message = "请输入当前密码")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 50, message = "密码长度6-50位")
    private String newPassword;
}

package com.campus.secondhand.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 修改密码表单
 */
@Data
public class PasswordDTO {

    @NotBlank(message = "请输入原密码")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 20, message = "新密码长度需在 6-20 位之间")
    private String newPassword;

    @NotBlank(message = "请再次输入新密码")
    private String confirmPassword;
}
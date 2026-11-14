package com.campus.secondhand.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 注册表单
 */
@Data
public class RegisterDTO {

    @NotBlank(message = "登录名不能为空")
    @Size(min = 4, max = 20, message = "登录名长度需在 4-20 位之间")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{3,19}$", message = "登录名以字母开头，只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 20, message = "昵称不能超过 20 个字符")
    private String nickname;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 20, message = "校区名称过长")
    private String campus;

    @Size(max = 20, message = "学号过长")
    private String studentNo;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6-20 位之间")
    private String password;

    @NotBlank(message = "请再次输入密码")
    private String confirmPassword;
}
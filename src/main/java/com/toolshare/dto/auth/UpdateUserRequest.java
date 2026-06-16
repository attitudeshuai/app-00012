package com.toolshare.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @Size(max = 50, message = "用户名长度不能超过50")
    private String username;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100")
    private String email;

    @Size(max = 500, message = "头像URL长度不能超过500")
    private String avatar;

    @Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
    private String password;
}

package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户登录请求")
public class LoginFormDTO {
    @Schema(description = "手机号", example = "18909233524")
    private String phone;

    @Schema(description = "短信验证码", example = "123456")
    private String code;

    @Schema(description = "登录密码", example = "123456")
    private String password;
}

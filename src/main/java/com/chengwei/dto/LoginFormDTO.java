package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import static com.chengwei.utils.common.RegexPatterns.PHONE_REGEX;

@Data
@Schema(description = "用户登录请求")
public class LoginFormDTO {
    @Schema(description = "手机号", example = "18909233524")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = PHONE_REGEX, message = "手机号格式错误")
    private String phone;

    @Schema(description = "短信验证码", example = "123456")
    private String code;

    @Schema(description = "登录密码", example = "123456")
    private String password;
}

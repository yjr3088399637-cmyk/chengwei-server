package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Schema(description = "店员或店长登录请求")
public class ClerkLoginFormDTO {
    @Schema(description = "店员账号", example = "manager02")
    @NotBlank(message = "店员账号不能为空")
    @Size(max = 32, message = "店员账号长度不能超过 32 位")
    private String username;

    @Schema(description = "登录密码", example = "123456")
    @NotBlank(message = "登录密码不能为空")
    @Size(min = 6, max = 20, message = "登录密码长度必须在 6-20 位之间")
    private String password;
}

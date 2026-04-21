package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "店员或店长登录请求")
public class ClerkLoginFormDTO {
    @Schema(description = "店员账号", example = "manager02")
    private String username;

    @Schema(description = "登录密码", example = "123456")
    private String password;
}

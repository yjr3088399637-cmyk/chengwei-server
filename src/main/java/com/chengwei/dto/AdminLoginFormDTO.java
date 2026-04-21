package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员登录请求")
public class AdminLoginFormDTO {
    @Schema(description = "管理员账号", example = "admin001")
    private String username;

    @Schema(description = "管理员密码", example = "123456")
    private String password;
}

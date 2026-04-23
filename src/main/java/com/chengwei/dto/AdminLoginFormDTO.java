package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Schema(description = "管理员登录请求")
public class AdminLoginFormDTO {
    @Schema(description = "管理员账号", example = "admin001")
    @NotBlank(message = "管理员账号不能为空")
    @Size(max = 32, message = "管理员账号长度不能超过 32 位")
    private String username;

    @Schema(description = "管理员密码", example = "123456")
    @NotBlank(message = "管理员密码不能为空")
    @Size(min = 6, max = 20, message = "管理员密码长度必须在 6-20 位之间")
    private String password;
}

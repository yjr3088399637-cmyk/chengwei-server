package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Schema(description = "修改密码请求")
public class ChangePasswordDTO {
    @Schema(description = "旧密码", example = "123456")
    @NotBlank(message = "旧密码不能为空")
    @Size(min = 6, max = 20, message = "旧密码长度必须在 6-20 位之间")
    private String oldPassword;

    @Schema(description = "新密码", example = "654321")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度必须在 6-20 位之间")
    private String newPassword;
}

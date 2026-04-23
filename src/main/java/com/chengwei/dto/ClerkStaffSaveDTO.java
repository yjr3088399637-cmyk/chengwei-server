package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Schema(description = "店长创建员工请求")
public class ClerkStaffSaveDTO {
    @Schema(description = "员工账号", example = "clerk18")
    @NotBlank(message = "员工账号不能为空")
    @Size(max = 32, message = "员工账号长度不能超过 32 位")
    private String username;

    @Schema(description = "员工初始密码", example = "123456")
    @NotBlank(message = "员工初始密码不能为空")
    @Size(min = 6, max = 20, message = "员工初始密码长度必须在 6-20 位之间")
    private String password;

    @Schema(description = "员工名称", example = "18号店员")
    @NotBlank(message = "员工名称不能为空")
    @Size(max = 32, message = "员工名称长度不能超过 32 个字符")
    private String name;
}

package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "店长创建员工请求")
public class ClerkStaffSaveDTO {
    @Schema(description = "员工账号", example = "clerk18")
    private String username;

    @Schema(description = "员工初始密码", example = "123456")
    private String password;

    @Schema(description = "员工名称", example = "18号店员")
    private String name;
}

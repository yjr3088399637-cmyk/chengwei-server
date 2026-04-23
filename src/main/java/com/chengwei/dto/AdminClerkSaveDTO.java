package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Schema(description = "管理员创建店长请求")
public class AdminClerkSaveDTO {
    @Schema(description = "店铺 ID", example = "18")
    @NotNull(message = "请选择店铺")
    private Long shopId;

    @Schema(description = "店长账号", example = "manager18")
    @NotBlank(message = "店长账号不能为空")
    @Size(max = 32, message = "店长账号长度不能超过 32 位")
    private String username;

    @Schema(description = "店长初始密码", example = "123456")
    @NotBlank(message = "店长密码不能为空")
    @Size(min = 6, max = 20, message = "店长密码长度必须在 6-20 位之间")
    private String password;

    @Schema(description = "店长名称", example = "18号店长")
    @NotBlank(message = "店长名称不能为空")
    @Size(max = 32, message = "店长名称长度不能超过 32 个字符")
    private String name;
}

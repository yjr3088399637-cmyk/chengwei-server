package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员创建店长请求")
public class AdminClerkSaveDTO {
    @Schema(description = "店铺 ID", example = "18")
    private Long shopId;

    @Schema(description = "店长账号", example = "manager18")
    private String username;

    @Schema(description = "店长初始密码", example = "123456")
    private String password;

    @Schema(description = "店长名称", example = "18号店长")
    private String name;
}

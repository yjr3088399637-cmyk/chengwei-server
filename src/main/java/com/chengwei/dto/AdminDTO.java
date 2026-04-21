package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员简要信息")
public class AdminDTO {
    @Schema(description = "管理员 ID", example = "1")
    private Long id;

    @Schema(description = "管理员账号", example = "admin001")
    private String username;

    @Schema(description = "管理员名称", example = "平台管理员")
    private String name;
}

package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "当前登录店员简要信息")
public class ClerkDTO {
    @Schema(description = "店员 ID", example = "1")
    private Long id;

    @Schema(description = "所属店铺 ID", example = "10")
    private Long shopId;

    @Schema(description = "账号", example = "manager02")
    private String username;

    @Schema(description = "姓名", example = "2号店长")
    private String name;

    @Schema(description = "角色，1店长 2店员", example = "1")
    private Integer role;
}

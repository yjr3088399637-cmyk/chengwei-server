package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "本店员工展示对象")
public class ClerkStaffVO {
    @Schema(description = "店员 ID", example = "2")
    private Long id;

    @Schema(description = "所属店铺 ID", example = "10")
    private Long shopId;

    @Schema(description = "账号", example = "clerk02")
    private String username;

    @Schema(description = "姓名", example = "2号店员")
    private String name;

    @Schema(description = "角色，1店长 2店员", example = "2")
    private Integer role;

    @Schema(description = "状态，1启用 0停用", example = "1")
    private Integer status;
}

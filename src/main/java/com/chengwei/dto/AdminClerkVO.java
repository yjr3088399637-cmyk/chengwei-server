package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理端店长展示对象")
public class AdminClerkVO {
    @Schema(description = "店长 ID", example = "18")
    private Long id;

    @Schema(description = "所属店铺 ID", example = "18")
    private Long shopId;

    @Schema(description = "所属店铺名称", example = "TONI&GUY（上海东方广场店）")
    private String shopName;

    @Schema(description = "账号", example = "manager18")
    private String username;

    @Schema(description = "姓名", example = "店长18")
    private String name;

    @Schema(description = "角色，1店长 2店员", example = "1")
    private Integer role;

    @Schema(description = "状态，1启用 0停用", example = "1")
    private Integer status;
}

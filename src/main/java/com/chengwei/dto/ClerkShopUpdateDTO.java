package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "店长修改店铺资料请求")
public class ClerkShopUpdateDTO {
    @Schema(description = "店铺名称", example = "城味小馆（万达店）")
    private String name;

    @Schema(description = "店铺图片，多个链接用英文逗号分隔", example = "https://example.com/1.jpg,https://example.com/2.jpg")
    private String images;

    @Schema(description = "商圈", example = "拱墅万达")
    private String area;

    @Schema(description = "店铺地址", example = "杭行路666号万达广场4层")
    private String address;

    @Schema(description = "人均价格", example = "88")
    private Long avgPrice;

    @Schema(description = "营业时间", example = "10:00-22:00")
    private String openHours;
}

package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

@Data
@Schema(description = "店长修改店铺资料请求")
public class ClerkShopUpdateDTO {
    @Schema(description = "店铺名称", example = "城味小馆（万达店）")
    @Size(max = 64, message = "店铺名称长度不能超过 64 个字符")
    private String name;

    @Schema(description = "店铺图片，多个链接用英文逗号分隔", example = "https://example.com/1.jpg,https://example.com/2.jpg")
    @Size(max = 2048, message = "图片链接总长度不能超过 2048 个字符")
    private String images;

    @Schema(description = "商圈", example = "拱墅万达")
    @Size(max = 64, message = "商圈长度不能超过 64 个字符")
    private String area;

    @Schema(description = "店铺地址", example = "杭行路666号万达广场4层")
    @Size(max = 255, message = "店铺地址长度不能超过 255 个字符")
    private String address;

    @Schema(description = "人均价格", example = "88")
    @PositiveOrZero(message = "人均价格不能为负数")
    private Long avgPrice;

    @Schema(description = "营业时间", example = "10:00-22:00")
    @Size(max = 32, message = "营业时间长度不能超过 32 个字符")
    private String openHours;
}
